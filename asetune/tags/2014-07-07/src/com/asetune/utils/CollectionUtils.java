package com.asetune.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CollectionUtils
{
//	/**
//	 * Sort a map based on the Value
//	 * @param map the map that we will sort, the value needs to implement Comparable interface
//	 * @param ascending should we do ascending or descending sort
//	 * @return A TreeMap that is sorted on the values in the map.
//	 */
//	public static <K, V extends Comparable<V>> Map<K, V> sortByValuesDesc(final Map<K, V> map, final boolean ascending)
//	{
//		// Create the comparator
//		Comparator<K> valueComparator = new Comparator<K>()
//		{
//			@Override
//			public int compare(K k1, K k2)
//			{
//				V v1 = map.get(k1);
//				V v2 = map.get(k2);
//
//				int compare = v1.compareTo(v2);
//				
//				if (ascending)
//					return compare;
//
//				// descending sort
//				if ( compare > 0 ) return -1;
//				if ( compare < 0 ) return 1;
//				return 0;
//			}
//		};
//
//		// make the sort
//		Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
//		sortedByValues.putAll(map);
//
//		return sortedByValues;
//	}

	/**
	 * Sort a map based on the Value
	 * @param map the map that we will sort, the value needs to implement Comparable interface
	 * @param ascending should we do ascending or descending sort
	 * @return A LinkedHashMap that is sorted on the values in the map.
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByMapValue(Map<K, V> map, final boolean ascending)
	{
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>()
		{
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
			{
				if (ascending)
					return (o1.getValue()).compareTo(o2.getValue());
				else
					return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list)
		{
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * 
	 * @param collection
	 * @return
	 */
	public static boolean isNullOrEmpty(Collection<?> collection)
	{
		if (collection == null)
			return true;

		if (collection.size() == 0)
			return true;

		return false;
	}
	public static boolean isNullOrEmpty(Map<?,?> collection)
	{
		if (collection == null)
			return true;

		if (collection.size() == 0)
			return true;

		return false;
	}
}
