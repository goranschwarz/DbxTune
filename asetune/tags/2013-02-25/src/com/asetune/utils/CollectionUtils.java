package com.asetune.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class CollectionUtils
{
	/**
	 * Sort a map based on the Value
	 * @param map the map that we will sort, the value needs to implement Comparable interface
	 * @param ascending should we do ascending or descending sort
	 * @return A TreeMap that is sorted on the values in the map.
	 */
	public static <K, V extends Comparable<V>> Map<K, V> sortByValuesDesc(final Map<K, V> map, final boolean ascending)
	{
		// Create the comparator
		Comparator<K> valueComparator = new Comparator<K>()
		{
			@Override
			public int compare(K k1, K k2)
			{
				V v1 = map.get(k1);
				V v2 = map.get(k2);

				int compare = v1.compareTo(v2);
				
				if (ascending)
					return compare;

				// descending sort
				if ( compare > 0 ) return -1;
				if ( compare < 0 ) return 1;
				return 0;
			}
		};

		// make the sort
		Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
		sortedByValues.putAll(map);

		return sortedByValues;
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
