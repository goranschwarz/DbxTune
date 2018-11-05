package com.asetune.graph;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.asetune.graph.TrendGraphDataPoint.LabelType;

public class TrendGraphDataPointTest
{

	@Test
	public void testStaticLabels()
	{
		TrendGraphDataPoint tgdp = new TrendGraphDataPoint("dummyGraph", "dummyGraphLabel", TrendGraphDataPoint.Category.OTHER, false, false, new String[] {"L-1", "L-2", "L-3", "L-4", "L-5"}, LabelType.Static );

		Double[] dArray  = new Double[5];
		
		dArray[0]  = 1.0;
		dArray[1]  = 2.0;
		dArray[2]  = 3.0;
		dArray[3]  = 4.0;
		dArray[4]  = 5.0;

		// Set the values
		java.util.Date now = new java.util.Date();
		tgdp.setDataPoint(now, dArray);
		
		// Check name and date
		assertEquals("dummyGraph", tgdp.getName());
		assertEquals(now, tgdp.getDate());

		// Check Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// check data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0}, tgdp.getData());
	}

	@Test
	public void testStaticLabelsWithAdd()
	{
		TrendGraphDataPoint tgdp = new TrendGraphDataPoint("dummyGraph", "dummyGraphLabel", TrendGraphDataPoint.Category.OTHER, false, false, new String[] {"L-1", "L-2", "L-3", "L-4", "L-5"}, LabelType.Static );

		// Set the values
		java.util.Date now = new java.util.Date();
		tgdp.setDataPoint(now, new Double[] {1.0, 2.0, 3.0, 4.0, 5.0});

		//---- CHECK -------------------------
		// name
		assertEquals("dummyGraph", tgdp.getName());
		assertEquals(now, tgdp.getDate());

		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0}, tgdp.getData());

		
		//-----------------------------------------------------
		// NOW ADD a new label at the end
		//-----------------------------------------------------
		// Set the values, with one new label and data
		now = new java.util.Date();
		tgdp.setDataPoint(now,                  new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6"}, 
		                                        new Double[] { 1.0,   2.0,   3.0,   4.0,   5.0,   6.0});

		//---- CHECK -------------------------
		assertEquals(now, tgdp.getDate());
		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0}, tgdp.getData());

	
		//-----------------------------------------------------
		// Same date set, but the labels/data in another order... they should end up in the same output order, meaning the setDataPoint() will reorder the data to the correct "slot"
		//-----------------------------------------------------
		// Set the values, with one new label and data
		tgdp.setDataPoint(new java.util.Date(), new String[] {"L-7", "L-2", "L-3", "L-1", "L-4", "L-6", "L-5"}, 
		                                        new Double[] { 7.0,   2.0,   3.0,   1.0,   4.0,   6.0,   5.0});

		//---- CHECK -------------------------
		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6", "L-7"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}, tgdp.getData());
	}

	@Test
	public void testRuntimeReplacedLabels()
	{
		TrendGraphDataPoint tgdp = new TrendGraphDataPoint("dummyGraph", "dummyGraphLabel", TrendGraphDataPoint.Category.OTHER, false, false, TrendGraphDataPoint.RUNTIME_REPLACED_LABELS, LabelType.Dynamic );

		// Set the values, with one new label and data
		tgdp.setDataPoint(new java.util.Date(), new String[] {"L-1", "L-2", "L-3", "L-4", "L-5"}, 
		                                        new Double[] { 1.0,   2.0,   3.0,   4.0,   5.0});

		//---- CHECK -------------------------
		// name
		assertEquals("dummyGraph", tgdp.getName());

		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0}, tgdp.getData());

		//-----------------------------------------------------
		// Same date set, but the labels/data in another order... they should end up in the same output order, meaning the setDataPoint() will reorder the data to the correct "slot"
		//-----------------------------------------------------
		// Set the values, with one new label and data
		tgdp.setDataPoint(new java.util.Date(), new String[] {"L-6", "L-7", "L-2", "L-3", "L-1", "L-4", "L-8", "L-5"}, 
		                                        new Double[] { 6.0,   7.0,   2.0,   3.0,   1.0,   4.0,   8.0,   5.0});

		//---- CHECK -------------------------
		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6", "L-7", "L-8"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, tgdp.getData());
	}


	@Test
	public void testRuntimeReplacedLabelsDisplay()
	{
		TrendGraphDataPoint tgdp = new TrendGraphDataPoint("dummyGraph", "dummyGraphLabel", TrendGraphDataPoint.Category.OTHER, false, false, TrendGraphDataPoint.RUNTIME_REPLACED_LABELS, LabelType.Dynamic );

		// Set the values, with one new label and data
		tgdp.setDataPoint(new java.util.Date(), new String[] {"L-1", "L-2", "L-3", "L-4", "L-5"}, 
		                                        new Double[] { 1.0,   2.0,   3.0,   4.0,   5.0});

		//---- CHECK -------------------------
		// name
		assertEquals("dummyGraph", tgdp.getName());

		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0}, tgdp.getData());

		//-----------------------------------------------------
		// Add L-6
		//-----------------------------------------------------
		// Set the values, with one new label and data
		tgdp.setDataPoint(new java.util.Date(), new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6"},
		                                        new String[] {"L1d", "L2d", "L3d", "L4d", "L5d", "L6d"},
		                                        new Double[] { 1.0,   2.0,   3.0,   4.0,   5.0,   6.0});

		//---- CHECK -------------------------
		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6"}, tgdp.getLabel());
		assertArrayEquals(new String[] {"L1d", "L2d", "L3d", "L4d", "L5d", "L6d"}, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0}, tgdp.getData());

	
		//-----------------------------------------------------
		// L-6 in another order, add L-7
		//-----------------------------------------------------
		// Set the values, with one new label and data
		tgdp.setDataPoint(new java.util.Date(), new String[] {"L-1", "L-2", "L-3", "L-6", "L-4", "L-5", "L-7"},
		                                        new String[] {"L1d", "L2d", "L3d", "L6d", "L4d", "L5d", "L7d"},
		                                        new Double[] { 1.0,   2.0,   3.0,   6.0,   4.0,   5.0,   7.0});

		//---- CHECK -------------------------
		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6", "L-7"}, tgdp.getLabel());
		assertArrayEquals(new String[] {"L1d", "L2d", "L3d", "L4d", "L5d", "L6d", "L7d"}, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}, tgdp.getData());

	
	
		//-----------------------------------------------------
		// Change the Display labels (for some of the labels)
		//-----------------------------------------------------
		// Set the values, with one new label and data
		tgdp.setDataPoint(new java.util.Date(), new String[] {"L-1", "L-2", "L-3", "L-6", "L-4", "L-5", "L-7"},
		                                        new String[] {"L1a", "L2d", "L3b", "L6d", "L4c", "L5d", "L7e"},
		                                        new Double[] { 1.0,   2.0,   3.0,   6.0,   4.0,   5.0,   7.0});

		//---- CHECK -------------------------
		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6", "L-7"}, tgdp.getLabel());
		assertArrayEquals(new String[] {"L1a", "L2d", "L3b", "L4c", "L5d", "L6d", "L7e"}, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}, tgdp.getData());
	

	
		//-----------------------------------------------------
		// Make the data set smaller: same labels/display-labels should be there, but the data slot should be 0 (or possibly null if we decide to implement that in the future)
		//-----------------------------------------------------
		// Set the values, with one new label and data
		tgdp.setDataPoint(new java.util.Date(), new String[] {"L-3", "L-2"},
		                                        new String[] {"L3x", "L2x"},
		                                        new Double[] { 3.0,   2.0 });

		//---- CHECK -------------------------
		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6", "L-7"}, tgdp.getLabel());
		assertArrayEquals(new String[] {"L1a", "L2x", "L3x", "L4c", "L5d", "L6d", "L7e"}, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {0.0, 2.0, 3.0, 0.0, 0.0, 0.0, 0.0}, tgdp.getData());
	
	
	}
	
	
	@Test
	public void testMap()
	{
		TrendGraphDataPoint tgdp = new TrendGraphDataPoint("dummyGraph", "dummyGraphLabel", TrendGraphDataPoint.Category.OTHER, false, false, TrendGraphDataPoint.RUNTIME_REPLACED_LABELS, LabelType.Dynamic );

		//-----------------------------------------------------
		// Simple
		//-----------------------------------------------------
		// Set the values
		java.util.Date now = new java.util.Date();

		Map<String, Double> dataMap = new LinkedHashMap<String, Double>();
		dataMap.put("L-1", 1.0);
		dataMap.put("L-2", 2.0);
		dataMap.put("L-3", 3.0);
		tgdp.setData(now, dataMap);

		
		//---- CHECK -------------------------
		// name
		assertEquals("dummyGraph", tgdp.getName());
		assertEquals(now, tgdp.getDate());

		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0}, tgdp.getData());

	
	
		//-----------------------------------------------------
		// In another order
		//-----------------------------------------------------
		dataMap = new LinkedHashMap<String, Double>();
		dataMap.put("L-3", 3.0);
		dataMap.put("L-1", 1.0);
		dataMap.put("L-2", 2.0);
		tgdp.setData(now, dataMap);

		
		//---- CHECK -------------------------
		// name
		assertEquals("dummyGraph", tgdp.getName());
		assertEquals(now, tgdp.getDate());

		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0}, tgdp.getData());

	
	
		//-----------------------------------------------------
		// just L-3
		//-----------------------------------------------------
		dataMap = new LinkedHashMap<String, Double>();
		dataMap.put("L-3", 3.0);
		tgdp.setData(now, dataMap);

		
		//---- CHECK -------------------------
		// name
		assertEquals("dummyGraph", tgdp.getName());
		assertEquals(now, tgdp.getDate());

		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {0.0, 0.0, 3.0}, tgdp.getData());
	

		
		
		//-----------------------------------------------------
		// add L4, L5, L6  (L1-3 should be 0.0)
		//-----------------------------------------------------
		dataMap = new LinkedHashMap<String, Double>();
		dataMap.put("L-4", 4.0);
		dataMap.put("L-5", 5.0);
		dataMap.put("L-6", 6.0);
		tgdp.setData(now, dataMap);

		
		//---- CHECK -------------------------
		// name
		assertEquals("dummyGraph", tgdp.getName());
		assertEquals(now, tgdp.getDate());

		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3", "L-4", "L-5", "L-6"}, tgdp.getLabel());
		assertArrayEquals(null, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {0.0, 0.0, 0.0, 4.0, 5.0, 6.0}, tgdp.getData());
	
	}

	@Test
	public void testMapLabelDisplay()
	{
		TrendGraphDataPoint tgdp = new TrendGraphDataPoint("dummyGraph", "dummyGraphLabel", TrendGraphDataPoint.Category.OTHER, false, false, TrendGraphDataPoint.RUNTIME_REPLACED_LABELS, LabelType.Dynamic );

		// Set the values
		java.util.Date now = new java.util.Date();

		Map<String, Double> dataMap  = new LinkedHashMap<String, Double>();
		Map<String, String> labelMap = new LinkedHashMap<String, String>();
		dataMap.put("L-1", 1.0); labelMap.put("L-1", "L1a");
		dataMap.put("L-2", 2.0); labelMap.put("L-2", "L2b");
		dataMap.put("L-3", 3.0); labelMap.put("L-3", "L3c");
		tgdp.setData(now, dataMap, labelMap);

		
		//---- CHECK -------------------------
		// name
		assertEquals("dummyGraph", tgdp.getName());
		assertEquals(now, tgdp.getDate());

		// Labels
		assertArrayEquals(new String[] {"L-1", "L-2", "L-3"}, tgdp.getLabel());
		assertArrayEquals(new String[] {"L1a", "L2b", "L3c"}, tgdp.getLabelDisplay());

		// Data
		assertArrayEquals(new Double[] {1.0, 2.0, 3.0}, tgdp.getData());
	}
}
