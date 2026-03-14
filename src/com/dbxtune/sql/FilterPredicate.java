/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.sql;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.SQLXML;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a runtime-evaluated filter predicate whose value type is
 * determined by JDBC metadata rather than Java generics.
 * <p>
 * A {@code FilterPredicate} combines:
 * <ul>
 *   <li>a field description ({@link FieldMetadata})</li>
 *   <li>a comparison operator ({@link Operator})</li>
 *   <li>a raw, untyped comparison value ({@code Object})</li>
 * </ul>
 *
 * <p>
 * The actual data type of both the candidate value and the predicate value
 * is resolved at runtime using a {@link JdbcDataType}. This allows predicates
 * to be constructed dynamically from {@link java.sql.ResultSetMetaData},
 * configuration files, or user input while maintaining consistent comparison
 * semantics.
 *
 * <h2>Type Conversion</h2>
 * <p>
 * Before evaluation, values are converted using the field's
 * {@link JdbcDataType}:
 * <ul>
 *   <li>Scalar operators convert a single value</li>
 *   <li>{@link Operator#IN} converts each element in a collection</li>
 *   <li>{@link Operator#BETWEEN} converts both range bounds</li>
 * </ul>
 *
 * <p>
 * Conversion errors or incompatible operator/value combinations result in
 * {@link IllegalArgumentException}.
 *
 * <h2>Supported Operators</h2>
 * <ul>
 *   <li>{@link Operator#EQUALS}</li>
 *   <li>{@link Operator#GREATER_THAN}</li>
 *   <li>{@link Operator#LESS_THAN}</li>
 *   <li>{@link Operator#IN}</li>
 *   <li>{@link Operator#BETWEEN}</li>
 *   <li>{@link Operator#LIKE} (SQL-style {@code %} and {@code _} wildcards)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is immutable and therefore thread-safe, assuming the supplied
 * {@link JdbcDataType} and {@link Operator} implementations are thread-safe.
 *
 * <h2>Examples</h2>
 *
 * <pre>{@code
 * // Using JDBC type constants
 * FilterPredicate amountFilter =
 *     new FilterPredicate(
 *         new FieldMetadata("amount", Types.DECIMAL),
 *         Operator.BETWEEN,
 *         new Range("100.00", "500.00")
 *     );
 *
 * boolean matches = amountFilter.test(new BigDecimal("250.00"));
 * }</pre>
 *
 * <pre>{@code
 * // LIKE operator
 * FilterPredicate nameFilter =
 *     new FilterPredicate(
 *         new FieldMetadata("name", JdbcDataTypes.VARCHAR),
 *         Operator.LIKE,
 *         "Joh%"
 *     );
 * }</pre>
 *
 * @see FieldMetadata
 * @see JdbcDataType
 * @see JdbcDataTypes
 * @see Operator
 * @see Range
 */
public final class FilterPredicate 
{
	private final FieldMetadata _metadata;
	private final Operator      _operator;
	private final Object        _value;

	public FilterPredicate(FieldMetadata metadata, Operator operator, Object value) 
	{
		_metadata = metadata;
		_operator = operator;
		_value    = value;
	}

	public boolean test(Object candidateValue) 
	{
		JdbcDataType type  = _metadata.getDataType();
		Object       left  = type.convert(candidateValue);
		Object       right = convertRight(type, _value);

		return _operator.apply(left, right);
	}

	private Object convertRight(JdbcDataType type, Object value) 
	{
		if (_operator == Operator.IN) 
		{
			if (!(value instanceof Collection)) 
			{
				throw new IllegalArgumentException("IN requires a Collection");
			}

			Collection<?> values = (Collection<?>) value;
			return values.stream()
					.map(type::convert)
					.collect(Collectors.toSet());
		}

		if (_operator == Operator.BETWEEN) 
		{
			if (!(value instanceof Range)) 
			{
				throw new IllegalArgumentException("BETWEEN requires a Range");
			}

			Range r = (Range) value;
			return new Range( type.convert(r.lower()), type.convert(r.upper()) );
		}

		return type.convert(value);
	}
	
	@Override
	public String toString() 
	{
		StringBuilder sb = new StringBuilder();

		sb.append("{")
			.append("colName='")
			.append(_metadata.getFieldName())
			.append("' ")
			.append(_operator.name())
			.append(" ")
			.append(formatValue(_value))
			.append("}")
			;

		return sb.toString();
	}

	private String formatValue(Object val) 
	{
		if (val == null) 
		{
			return "-NULL-";
		}

		if (_operator == Operator.IN && val instanceof Iterable) 
		{
			String values = ((Iterable<?>) val)
					.iterator()
					.hasNext()
					? ((Iterable<?>) val).toString()
					: "()";
			return values;
		}

		if (_operator == Operator.BETWEEN && val instanceof Range) 
		{
			Range r = (Range) val;
			return r.lower() + " AND " + r.upper();
		}

		if (_operator == Operator.LIKE || _operator == Operator.REGEX) 
		{
			return "'" + val + "'";
		}

		if (val instanceof String) 
		{
			return "'" + val + "'";
		}

		return String.valueOf(val);
	}
	

	//------------------------------------------------------------------------------
	//-- 
	//------------------------------------------------------------------------------
	public interface JdbcDataType 
	{
		int      jdbcType();
		Class<?> javaType();
		Object   convert(Object value);
	}

	//------------------------------------------------------------------------------
	//-- 
	//------------------------------------------------------------------------------
	public enum JdbcDataTypes implements JdbcDataType 
	{
		// -------------- Character Types --------------
		CHAR                   (Types.CHAR,         String.class, Object::toString),
		VARCHAR                (Types.VARCHAR,      String.class, Object::toString),
		LONGVARCHAR            (Types.LONGVARCHAR,  String.class, Object::toString),
		NCHAR                  (Types.NCHAR,        String.class, Object::toString),
		NVARCHAR               (Types.NVARCHAR,     String.class, Object::toString),
		LONGNVARCHAR           (Types.LONGNVARCHAR, String.class, Object::toString),

		// -------------- Numeric Types --------------
		INTEGER                (Types.INTEGER,  Integer.class, v -> Integer.valueOf(v.toString())),
		SMALLINT               (Types.SMALLINT, Short.class,   v -> Short.valueOf(v.toString())),
		TINYINT                (Types.TINYINT,  Byte.class,    v -> Byte.valueOf(v.toString())),
		BIGINT                 (Types.BIGINT,   Long.class,    v -> Long.valueOf(v.toString())),
					           
		FLOAT                  (Types.FLOAT,    Double.class,  v -> Double.valueOf(v.toString())),
		REAL                   (Types.REAL,     Float.class,   v -> Float.valueOf(v.toString())),
		DOUBLE                 (Types.DOUBLE,   Double.class,  v -> Double.valueOf(v.toString())),
					           
		NUMERIC                (Types.NUMERIC,  BigDecimal.class, JdbcDataTypes::toBigDecimal),
		DECIMAL                (Types.DECIMAL,  BigDecimal.class, JdbcDataTypes::toBigDecimal),

		// -------------- Boolean --------------
		BOOLEAN                (Types.BOOLEAN, Boolean.class, JdbcDataTypes::toBoolean),
		BIT                    (Types.BIT,     Boolean.class, JdbcDataTypes::toBoolean),

		// -------------- Temporal --------------
		DATE                   (Types.DATE,                    LocalDate.class,      JdbcDataTypes::toLocalDate),
		TIME                   (Types.TIME,                    LocalTime.class,      JdbcDataTypes::toLocalTime),
		TIMESTAMP              (Types.TIMESTAMP,               LocalDateTime.class,  JdbcDataTypes::toLocalDateTime),
		TIMESTAMP_WITH_TIMEZONE(Types.TIMESTAMP_WITH_TIMEZONE, OffsetDateTime.class, JdbcDataTypes::toOffsetDateTime),

		// -------------- Binary / LOB --------------
		BINARY                 (Types.BINARY,        byte[].class, JdbcDataTypes::toBytes),
		VARBINARY              (Types.VARBINARY,     byte[].class, JdbcDataTypes::toBytes),
		LONGVARBINARY          (Types.LONGVARBINARY, byte[].class, JdbcDataTypes::toBytes),

		BLOB                   (Types.BLOB,  Blob.class,  v -> v),
		CLOB                   (Types.CLOB,  Clob.class,  v -> v),
		NCLOB                  (Types.NCLOB, NClob.class, v -> v),

		// -------------- Misc --------------
		UUID                   (Types.OTHER,       java.util.UUID.class, v -> java.util.UUID.fromString(v.toString())),
		ARRAY                  (Types.ARRAY,       Array.class,          v -> v),
		STRUCT                 (Types.STRUCT,      Struct.class,         v -> v),
		SQLXML                 (Types.SQLXML,      SQLXML.class,         v -> v),
		REF                    (Types.REF,         Ref.class,            v -> v),
		DATALINK               (Types.DATALINK,    java.net.URL.class,   v -> v),
		JAVA_OBJECT            (Types.JAVA_OBJECT, Object.class,         v -> v),

		NULL                   (Types.NULL,        Object.class,         v -> null);

		// -------------- Implementation --------------

		private final int       _jdbcType;
		private final Class<?>  _javaType;
		private final Converter _converter;

		JdbcDataTypes(int jdbcType, Class<?> javaType, Converter converter) 
		{
			_jdbcType  = jdbcType;
			_javaType  = javaType;
			_converter = converter;
		}

		@Override
		public int jdbcType() 
		{
			return _jdbcType;
		}

		@Override
		public Class<?> javaType() 
		{
			return _javaType;
		}

		@Override
		public Object convert(Object value) 
		{
			if (value == null) 
				return null;

			if (_javaType.isInstance(value)) 
				return value;

			return _converter.convert(value);
		}

		// -------------- Lookup by JDBC Type --------------
//		public static JdbcDataTypes fromJdbcType(int jdbcType) 
//		{
//			return Arrays.stream(values())
//				.filter(t -> t.jdbcType == jdbcType)
//				.findFirst()
//				.orElseThrow(() -> new IllegalArgumentException("Unsupported JDBC type: " + jdbcType));
//		}
		public static JdbcDataTypes fromJdbcType(int jdbcType) 
		{
			for (JdbcDataTypes type : values()) 
			{
				if (type.jdbcType() == jdbcType) 
				{
					return type;
				}
			}
			throw new IllegalArgumentException("Unsupported JDBC type: " + jdbcType);
		}

		// -------------- Converters --------------
		private static BigDecimal toBigDecimal(Object v) 
		{
			return (v instanceof BigDecimal) ? (BigDecimal) v : new BigDecimal(v.toString());
		}

		private static Boolean toBoolean(Object v) 
		{
			if (v instanceof Boolean) 
				return (Boolean) v;

			if (v instanceof Number) 
				return ((Number) v).intValue() != 0;

			return Boolean.parseBoolean(v.toString());
		}

		private static LocalDate toLocalDate(Object v) 
		{
			if (v instanceof Date) 
				return ((Date) v).toLocalDate();

			return LocalDate.parse(v.toString());
		}

		private static LocalTime toLocalTime(Object v) 
		{
			if (v instanceof Time) 
				return ((Time) v).toLocalTime();

			return LocalTime.parse(v.toString());
		}

		private static LocalDateTime toLocalDateTime(Object v) 
		{
			if (v instanceof Timestamp) 
				return ((Timestamp) v).toLocalDateTime();

			return LocalDateTime.parse(v.toString());
		}

		private static OffsetDateTime toOffsetDateTime(Object v) 
		{
			if (v instanceof OffsetDateTime) 
				return (OffsetDateTime) v;

			return OffsetDateTime.parse(v.toString());
		}

		private static byte[] toBytes(Object v) 
		{
			if (v instanceof byte[]) 
				return (byte[]) v;

			throw new IllegalArgumentException("Cannot convert to byte[]: " + v.getClass());
		}

		@FunctionalInterface
		private interface Converter 
		{
			Object convert(Object value);
		}
	}

	
	//------------------------------------------------------------------------------
	//-- 
	//------------------------------------------------------------------------------
	public static class Range 
	{
		private final Comparable<Object> _lower;
		private final Comparable<Object> _upper;

		@SuppressWarnings("unchecked")
		public Range(Object lower, Object upper) 
		{
			if (!(lower instanceof Comparable) || !(upper instanceof Comparable)) 
			{
				throw new IllegalArgumentException("Range bounds must be Comparable");
			}

			_lower = (Comparable<Object>) lower;
			_upper = (Comparable<Object>) upper;

			if (_lower.compareTo(_upper) > 0) 
			{
				throw new IllegalArgumentException("Lower bound > upper bound");
			}
		}

		public Comparable<Object> lower() 
		{
			return _lower;
		}

		public Comparable<Object> upper() 
		{
			return _upper;
		}
	}

	//------------------------------------------------------------------------------
	//-- 
	//------------------------------------------------------------------------------
	public enum Operator 
	{
		EQUALS 
		{
			@Override
			public boolean apply(Object left, Object right) 
			{
				return left.equals(right);
			}
		},

		GREATER_THAN 
		{
			@Override
			@SuppressWarnings("unchecked")
			public boolean apply(Object left, Object right) 
			{
				return ((Comparable<Object>) left).compareTo(right) > 0;
			}
		},

		LESS_THAN {
			@Override
			@SuppressWarnings("unchecked")
			public boolean apply(Object left, Object right) 
			{
				return ((Comparable<Object>) left).compareTo(right) < 0;
			}
		},

		IN 
		{
			@Override
			public boolean apply(Object left, Object right) 
			{
				if (!(right instanceof Collection)) 
				{
					throw new IllegalArgumentException("IN requires a Collection");
				}
				return ((Collection<?>) right).contains(left);
			}
		},

		BETWEEN 
		{
			@Override
			@SuppressWarnings("unchecked")
			public boolean apply(Object left, Object right) 
			{
				if (!(right instanceof Range)) 
				{
					throw new IllegalArgumentException("BETWEEN requires a Range");
				}

				Range range = (Range) right;
				Comparable<Object> value = (Comparable<Object>) left;

				return value.compareTo(range.lower()) >= 0 && value.compareTo(range.upper()) <= 0;
			}
		},

		LIKE 
		{
			@Override
			public boolean apply(Object left, Object right) 
			{
				if (!(left instanceof String) || !(right instanceof String)) 
				{
					throw new IllegalArgumentException("LIKE applies to Strings only");
				}
				return sqlLike((String) left, (String) right);
			}
		},
		
	    REGEX 
	    {
			@Override
			public boolean apply(Object left, Object right) 
			{
				if (!(left instanceof String) || !(right instanceof String)) 
				{
					throw new IllegalArgumentException("REGEX applies to Strings only");
				}
				return Pattern.matches((String) right, (String) left);
			}
	    };

		public abstract boolean apply(Object left, Object right);

		private static boolean sqlLike(String value, String pattern) 
		{
			String regex = pattern
					.replace(".", "\\.")
					.replace("_", ".")
					.replace("%", ".*");
			return Pattern.matches(regex, value);
		}
		
		public String symbol() 
		{
			switch (this) 
			{
				case EQUALS:       return "=";
				case GREATER_THAN: return ">";
				case LESS_THAN:    return "<";
				case IN:           return "IN";
				case BETWEEN:      return "BETWEEN";
				case LIKE:         return "LIKE";
				case REGEX:        return "REGEX";
				default:           return name();
			}
		}
	}

	//------------------------------------------------------------------------------
	//-- 
	//------------------------------------------------------------------------------
	public static class FieldMetadata 
	{

		private final String       _fieldName;
		private final JdbcDataType _dataType;

		public FieldMetadata(String fieldName, JdbcDataType dataType) 
		{
			_fieldName = fieldName;
			_dataType  = dataType;
		}
		
		public FieldMetadata(String fieldName, int jdbcType) 
		{
			this(fieldName, JdbcDataTypes.fromJdbcType(jdbcType));
		}


		public String getFieldName() 
		{
			return _fieldName;
		}

		public JdbcDataType getDataType() 
		{
			return _dataType;
		}
	}

	
	
//	public static void main(String[] args)
//	{
//		FieldMetadata meta = new FieldMetadata("amount", JdbcDataTypes.DECIMAL);
//
//		FilterPredicate p = new FilterPredicate(meta, Operator.GREATER_THAN, "100.00");
//
//		System.out.println(p.test(new BigDecimal("150.00"))); // true
//		
//	}
	
}
