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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

import com.dbxtune.sql.FilterPredicate;
import com.dbxtune.sql.FilterPredicate.FieldMetadata;
import com.dbxtune.sql.FilterPredicate.JdbcDataTypes;
import com.dbxtune.sql.FilterPredicate.Operator;
import com.dbxtune.sql.FilterPredicate.Range;

public class FilterPredicateTest {

    // -------------------- IN --------------------

    @Test
    public void testInOperatorWithStrings() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("status", JdbcDataTypes.VARCHAR),
                Operator.IN,
                Arrays.asList("NEW", "OPEN", "HOLD")
            );

        assertTrue(predicate.test("OPEN"));
        assertFalse(predicate.test("CLOSED"));
    }

    @Test
    public void testInOperatorWithNumericCoercion() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("id", JdbcDataTypes.BIGINT),
                Operator.IN,
                Arrays.asList("1", "2", "3")
            );

        assertTrue(predicate.test(2L));
        assertFalse(predicate.test(5L));
    }

    // -------------------- BETWEEN --------------------

    @Test
    public void testBetweenWithBigDecimal() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("amount", JdbcDataTypes.DECIMAL),
                Operator.BETWEEN,
                new Range("100.00", "500.00")
            );

        assertTrue(predicate.test(new BigDecimal("100.00")));
        assertTrue(predicate.test(new BigDecimal("250.00")));
        assertTrue(predicate.test(new BigDecimal("500.00")));
        assertFalse(predicate.test(new BigDecimal("99.99")));
        assertFalse(predicate.test(new BigDecimal("600.00")));
    }

    @Test
    public void testBetweenWithDates() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("created", JdbcDataTypes.DATE),
                Operator.BETWEEN,
                new Range("2024-01-01", "2024-12-31")
            );

        assertTrue(predicate.test(LocalDate.of(2024, 6, 1)));
        assertFalse(predicate.test(LocalDate.of(2023, 12, 31)));
    }

    // -------------------- LIKE --------------------

    @Test
    public void testLikePrefix() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("name", JdbcDataTypes.VARCHAR),
                Operator.LIKE,
                "Joh%"
            );

        assertTrue(predicate.test("John"));
        assertTrue(predicate.test("Johnny"));
        assertFalse(predicate.test("Alice"));
    }

    @Test
    public void testLikeContains() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("description", JdbcDataTypes.VARCHAR),
                Operator.LIKE,
                "%error%"
            );

        assertTrue(predicate.test("fatal error occurred"));
        assertFalse(predicate.test("all good"));
    }

    @Test
    public void testLikeSingleCharacterWildcard() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("code", JdbcDataTypes.VARCHAR),
                Operator.LIKE,
                "A_1"
            );

        assertTrue(predicate.test("AB1"));
        assertTrue(predicate.test("AC1"));
        assertFalse(predicate.test("A11"));
    }

    // -------------------- BASIC COMPARISONS --------------------

    @Test
    public void testGreaterThanWithCoercion() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("age", JdbcDataTypes.INTEGER),
                Operator.GREATER_THAN,
                18
            );

        assertTrue(predicate.test(21));
        assertFalse(predicate.test(16));
    }

    @Test
    public void testEquals() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("active", JdbcDataTypes.BOOLEAN),
                Operator.EQUALS,
                "true"
            );

        assertTrue(predicate.test(true));
        assertFalse(predicate.test(false));
    }

    // -------------------- FAILURE CASES --------------------

    @Test(expected = IllegalArgumentException.class)
    public void testLikeOnNonStringThrows() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("amount", JdbcDataTypes.DECIMAL),
                Operator.LIKE,
                "%10%"
            );

        predicate.test(new BigDecimal("100"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBetweenWithInvalidValueThrows() {
        FilterPredicate predicate =
            new FilterPredicate(
                new FieldMetadata("amount", JdbcDataTypes.DECIMAL),
                Operator.BETWEEN,
                "100-200"
            );

        predicate.test(new BigDecimal("150"));
    }
}
