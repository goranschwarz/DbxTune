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
package com.dbxtune.hostmon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.StringUtil;

/**
 * Take output from Window command 'typeperf' and make it into a table 
 * @author goran
 */
public class WindowsTypePerfCsvReader
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
//	private LinkedHashMap<String, TypeperfRow> _rowKeys  = new LinkedHashMap<>();
	private LinkedHashMap<Integer, CounterColumn> _inputRowDict = new LinkedHashMap<>();
	
	private LinkedHashSet<String> _counterGroupSet = new LinkedHashSet<>();

	private SimpleDateFormat _sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
	
	

	// Example:
	// gorans@GS-1-WIN C:\Users\gorans>typeperf -si 10 "\Processor(*)\*"
	// Results:
	// "(PDH-CSV 4.0)","\\GS-1-WIN\Processor(0)\% Processor Time","\\GS-1-WIN\Processor(0)\% User Time","\\GS-1-WIN\Processor(0)\% Privileged Time","\\GS-1-WIN\Processor(0)\Interrupts/sec","\\GS-1-WIN\Processor(0)\% DPC Time","\\GS-1-WIN\Processor(0)\% Interrupt Time","\\GS-1-WIN\Processor(0)\DPCs Queued/sec","\\GS-1-WIN\Processor(0)\DPC Rate","\\GS-1-WIN\Processor(0)\% Idle Time","\\GS-1-WIN\Processor(0)\% C1 Time","\\GS-1-WIN\Processor(0)\% C2 Time","\\GS-1-WIN\Processor(0)\% C3 Time","\\GS-1-WIN\Processor(0)\C1 Transitions/sec","\\GS-1-WIN\Processor(0)\C2 Transitions/sec","\\GS-1-WIN\Processor(0)\C3 Transitions/sec","\\GS-1-WIN\Processor(1)\% Processor Time","\\GS-1-WIN\Processor(1)\% User Time","\\GS-1-WIN\Processor(1)\% Privileged Time","\\GS-1-WIN\Processor(1)\Interrupts/sec","\\GS-1-WIN\Processor(1)\% DPC Time","\\GS-1-WIN\Processor(1)\% Interrupt Time","\\GS-1-WIN\Processor(1)\DPCs Queued/sec","\\GS-1-WIN\Processor(1)\DPC Rate","\\GS-1-WIN\Processor(1)\% Idle Time","\\GS-1-WIN\Processor(1)\% C1 Time","\\GS-1-WIN\Processor(1)\% C2 Time","\\GS-1-WIN\Processor(1)\% C3 Time","\\GS-1-WIN\Processor(1)\C1 Transitions/sec","\\GS-1-WIN\Processor(1)\C2 Transitions/sec","\\GS-1-WIN\Processor(1)\C3 Transitions/sec","\\GS-1-WIN\Processor(_Total)\% Processor Time","\\GS-1-WIN\Processor(_Total)\% User Time","\\GS-1-WIN\Processor(_Total)\% Privileged Time","\\GS-1-WIN\Processor(_Total)\Interrupts/sec","\\GS-1-WIN\Processor(_Total)\% DPC Time","\\GS-1-WIN\Processor(_Total)\% Interrupt Time","\\GS-1-WIN\Processor(_Total)\DPCs Queued/sec","\\GS-1-WIN\Processor(_Total)\DPC Rate","\\GS-1-WIN\Processor(_Total)\% Idle Time","\\GS-1-WIN\Processor(_Total)\% C1 Time","\\GS-1-WIN\Processor(_Total)\% C2 Time","\\GS-1-WIN\Processor(_Total)\% C3 Time","\\GS-1-WIN\Processor(_Total)\C1 Transitions/sec","\\GS-1-WIN\Processor(_Total)\C2 Transitions/sec","\\GS-1-WIN\Processor(_Total)\C3 Transitions/sec"
	// "12/29/2020 18:20:42.717","0.312142","0.156006","0.156006","109.233329","0.156006","0.000000","21.786702","0.000000","99.211091","99.211091","0.000000","0.000000","98.140100","0.000000","0.000000","0.000130","0.000000","0.000000","117.528267","0.000000","0.000000","7.495425","0.000000","99.538613","99.538613","0.000000","0.000000","92.543516","0.000000","0.000000","0.156136","0.078003","0.078003","226.761596","0.078003","0.000000","29.282128","0.000000","99.374852","99.374852","0.000000","0.000000","190.683616","0.000000","0.000000"
	// "12/29/2020 18:20:52.733","0.156559","0.000000","0.156005","111.127941","0.156005","0.000000","20.068927","0.000000","99.425786","99.425786","0.000000","0.000000","99.545873","0.000000","0.000000","0.000553","0.000000","0.000000","112.326086","0.000000","0.000000","1.797217","0.000000","99.697571","99.697571","0.000000","0.000000","91.757931","0.000000","0.000000","0.078556","0.000000","0.078003","223.454026","0.078003","0.000000","21.866145","0.000000","99.561678","99.561678","0.000000","0.000000","191.303804","0.000000","0.000000"
	// "12/29/2020 18:21:02.748","0.311650","0.156007","0.156007","120.410807","0.156007","0.000000","20.367997","1.000000","99.006794","99.006794","0.000000","0.000000","104.935123","0.000000","0.000000","0.935677","0.468020","0.468020","121.608924","0.000000","0.000000","2.396235","0.000000","98.503701","98.503701","0.000000","0.000000","98.744849","0.000000","0.000000","0.623664","0.312014","0.312014","242.019731","0.078003","0.000000","22.764232","1.000000","98.755247","98.755247","0.000000","0.000000","203.679972","0.000000","0.000000"
	// "12/29/2020 18:21:12.748","0.000038","0.000000","0.000000","113.502135","0.000000","0.000000","23.400440","0.000000","99.327128","99.327128","0.000000","0.000000","103.201941","0.000000","0.000000","0.000038","0.000000","0.000000","119.702252","0.000000","0.000000","7.000132","0.000000","99.538608","99.538608","0.000000","0.000000","94.601779","0.000000","0.000000","0.000038","0.000000","0.000000","233.204387","0.000000","0.000000","30.400572","0.000000","99.432868","99.432868","0.000000","0.000000","197.803721","0.000000","0.000000"

	// C:\Users\goran>typeperf -si 10 "\Processor(*)\*"
	// "(PDH-CSV 4.0)","\\GORANS2\Processor(0)\% Processor Time","\\GORANS2\Processor(0)\% User Time","\\GORANS2\Processor(0)\% Privileged Time","\\GORANS2\Processor(0)\Interrupts/sec","\\GORANS2\Processor(0)\% DPC Time","\\GORANS2\Processor(0)\% Interrupt Time","\\GORANS2\Processor(0)\DPCs Queued/sec","\\GORANS2\Processor(0)\DPC Rate","\\GORANS2\Processor(0)\% Idle Time","\\GORANS2\Processor(0)\% C1 Time","\\GORANS2\Processor(0)\% C2 Time","\\GORANS2\Processor(0)\% C3 Time","\\GORANS2\Processor(0)\C1 Transitions/sec","\\GORANS2\Processor(0)\C2 Transitions/sec","\\GORANS2\Processor(0)\C3 Transitions/sec","\\GORANS2\Processor(1)\% Processor Time","\\GORANS2\Processor(1)\% User Time","\\GORANS2\Processor(1)\% Privileged Time","\\GORANS2\Processor(1)\Interrupts/sec","\\GORANS2\Processor(1)\% DPC Time","\\GORANS2\Processor(1)\% Interrupt Time","\\GORANS2\Processor(1)\DPCs Queued/sec","\\GORANS2\Processor(1)\DPC Rate","\\GORANS2\Processor(1)\% Idle Time","\\GORANS2\Processor(1)\% C1 Time","\\GORANS2\Processor(1)\% C2 Time","\\GORANS2\Processor(1)\% C3 Time","\\GORANS2\Processor(1)\C1 Transitions/sec","\\GORANS2\Processor(1)\C2 Transitions/sec","\\GORANS2\Processor(1)\C3 Transitions/sec","\\GORANS2\Processor(2)\% Processor Time","\\GORANS2\Processor(2)\% User Time","\\GORANS2\Processor(2)\% Privileged Time","\\GORANS2\Processor(2)\Interrupts/sec","\\GORANS2\Processor(2)\% DPC Time","\\GORANS2\Processor(2)\% Interrupt Time","\\GORANS2\Processor(2)\DPCs Queued/sec","\\GORANS2\Processor(2)\DPC Rate","\\GORANS2\Processor(2)\% Idle Time","\\GORANS2\Processor(2)\% C1 Time","\\GORANS2\Processor(2)\% C2 Time","\\GORANS2\Processor(2)\% C3 Time","\\GORANS2\Processor(2)\C1 Transitions/sec","\\GORANS2\Processor(2)\C2 Transitions/sec","\\GORANS2\Processor(2)\C3 Transitions/sec","\\GORANS2\Processor(3)\% Processor Time","\\GORANS2\Processor(3)\% User Time","\\GORANS2\Processor(3)\% Privileged Time","\\GORANS2\Processor(3)\Interrupts/sec","\\GORANS2\Processor(3)\% DPC Time","\\GORANS2\Processor(3)\% Interrupt Time","\\GORANS2\Processor(3)\DPCs Queued/sec","\\GORANS2\Processor(3)\DPC Rate","\\GORANS2\Processor(3)\% Idle Time","\\GORANS2\Processor(3)\% C1 Time","\\GORANS2\Processor(3)\% C2 Time","\\GORANS2\Processor(3)\% C3 Time","\\GORANS2\Processor(3)\C1 Transitions/sec","\\GORANS2\Processor(3)\C2 Transitions/sec","\\GORANS2\Processor(3)\C3 Transitions/sec","\\GORANS2\Processor(4)\% Processor Time","\\GORANS2\Processor(4)\% User Time","\\GORANS2\Processor(4)\% Privileged Time","\\GORANS2\Processor(4)\Interrupts/sec","\\GORANS2\Processor(4)\% DPC Time","\\GORANS2\Processor(4)\% Interrupt Time","\\GORANS2\Processor(4)\DPCs Queued/sec","\\GORANS2\Processor(4)\DPC Rate","\\GORANS2\Processor(4)\% Idle Time","\\GORANS2\Processor(4)\% C1 Time","\\GORANS2\Processor(4)\% C2 Time","\\GORANS2\Processor(4)\% C3 Time","\\GORANS2\Processor(4)\C1 Transitions/sec","\\GORANS2\Processor(4)\C2 Transitions/sec","\\GORANS2\Processor(4)\C3 Transitions/sec","\\GORANS2\Processor(5)\% Processor Time","\\GORANS2\Processor(5)\% User Time","\\GORANS2\Processor(5)\% Privileged Time","\\GORANS2\Processor(5)\Interrupts/sec","\\GORANS2\Processor(5)\% DPC Time","\\GORANS2\Processor(5)\% Interrupt Time","\\GORANS2\Processor(5)\DPCs Queued/sec","\\GORANS2\Processor(5)\DPC Rate","\\GORANS2\Processor(5)\% Idle Time","\\GORANS2\Processor(5)\% C1 Time","\\GORANS2\Processor(5)\% C2 Time","\\GORANS2\Processor(5)\% C3 Time","\\GORANS2\Processor(5)\C1 Transitions/sec","\\GORANS2\Processor(5)\C2 Transitions/sec","\\GORANS2\Processor(5)\C3 Transitions/sec","\\GORANS2\Processor(6)\% Processor Time","\\GORANS2\Processor(6)\% User Time","\\GORANS2\Processor(6)\% Privileged Time","\\GORANS2\Processor(6)\Interrupts/sec","\\GORANS2\Processor(6)\% DPC Time","\\GORANS2\Processor(6)\% Interrupt Time","\\GORANS2\Processor(6)\DPCs Queued/sec","\\GORANS2\Processor(6)\DPC Rate","\\GORANS2\Processor(6)\% Idle Time","\\GORANS2\Processor(6)\% C1 Time","\\GORANS2\Processor(6)\% C2 Time","\\GORANS2\Processor(6)\% C3 Time","\\GORANS2\Processor(6)\C1 Transitions/sec","\\GORANS2\Processor(6)\C3 Transitions/sec","\\GORANS2\Processor(7)\% Processor Time","\\GORANS2\Processor(7)\% User Time","\\GORANS2\Processor(7)\% Privileged Time","\\GORANS2\Processor(7)\Interrupts/sec","\\GORANS2\Processor(7)\% DPC Time","\\GORANS2\Processor(7)\% Interrupt Time","\\GORANS2\Processor(7)\DPCs Queued/sec","\\GORANS2\Processor(7)\DPC Rate","\\GORANS2\Processor(7)\% Idle Time","\\GORANS2\Processor(7)\% C1 Time","\\GORANS2\Processor(7)\% C2 Time","\\GORANS2\Processor(7)\% C3 Time","\\GORANS2\Processor(7)\C1 Transitions/sec","\\GORANS2\Processor(7)\C2 Transitions/sec","\\GORANS2\Processor(7)\C3 Transitions/sec","\\GORANS2\Processor(8)\% Processor Time","\\GORANS2\Processor(8)\% User Time","\\GORANS2\Processor(8)\% Privileged Time","\\GORANS2\Processor(8)\Interrupts/sec","\\GORANS2\Processor(8)\% DPC Time","\\GORANS2\Processor(8)\% Interrupt Time","\\GORANS2\Processor(8)\DPCs Queued/sec","\\GORANS2\Processor(8)\DPC Rate","\\GORANS2\Processor(8)\% Idle Time","\\GORANS2\Processor(8)\% C1 Time","\\GORANS2\Processor(8)\% C2 Time","\\GORANS2\Processor(8)\% C3 Time","\\GORANS2\Processor(8)\C1 Transitions/sec","\\GORANS2\Processor(8)\C2 Transitions/sec","\\GORANS2\Processor(8)\C3 Transitions/sec","\\GORANS2\Processor(9)\% Processor Time","\\GORANS2\Processor(9)\% User Time","\\GORANS2\Processor(9)\% Privileged Time","\\GORANS2\Processor(9)\Interrupts/sec","\\GORANS2\Processor(9)\% DPC Time","\\GORANS2\Processor(9)\% Interrupt Time","\\GORANS2\Processor(9)\DPCs Queued/sec","\\GORANS2\Processor(9)\DPC Rate","\\GORANS2\Processor(9)\% Idle Time","\\GORANS2\Processor(9)\% C1 Time","\\GORANS2\Processor(9)\% C2 Time","\\GORANS2\Processor(9)\% C3 Time","\\GORANS2\Processor(9)\C1 Transitions/sec","\\GORANS2\Processor(9)\C2 Transitions/sec","\\GORANS2\Processor(9)\C3 Transitions/sec","\\GORANS2\Processor(10)\% Processor Time","\\GORANS2\Processor(10)\% User Time","\\GORANS2\Processor(10)\% Privileged Time","\\GORANS2\Processor(10)\Interrupts/sec","\\GORANS2\Processor(10)\% DPC Time","\\GORANS2\Processor(10)\% Interrupt Time","\\GORANS2\Processor(10)\DPCs Queued/sec","\\GORANS2\Processor(10)\DPC Rate","\\GORANS2\Processor(10)\% Idle Time","\\GORANS2\Processor(10)\% C1 Time","\\GORANS2\Processor(10)\% C2 Time","\\GORANS2\Processor(10)\% C3 Time","\\GORANS2\Processor(10)\C1 Transitions/sec","\\GORANS2\Processor(10)\C2 Transitions/sec","\\GORANS2\Processor(10)\C3 Transitions/sec","\\GORANS2\Processor(11)\% Processor Time","\\GORANS2\Processor(11)\% User Time","\\GORANS2\Processor(11)\% Privileged Time","\\GORANS2\Processor(11)\Interrupts/sec","\\GORANS2\Processor(11)\% DPC Time","\\GORANS2\Processor(11)\% Interrupt Time","\\GORANS2\Processor(11)\DPCs Queued/sec","\\GORANS2\Processor(11)\DPC Rate","\\GORANS2\Processor(11)\% Idle Time","\\GORANS2\Processor(11)\% C1 Time","\\GORANS2\Processor(11)\% C2 Time","\\GORANS2\Processor(11)\% C3 Time","\\GORANS2\Processor(11)\C1 Transitions/sec","\\GORANS2\Processor(11)\C2 Transitions/sec","\\GORANS2\Processor(11)\C3 Transitions/sec","\\GORANS2\Processor(_Total)\% Processor Time","\\GORANS2\Processor(_Total)\% User Time","\\GORANS2\Processor(_Total)\% Privileged Time","\\GORANS2\Processor(_Total)\Interrupts/sec","\\GORANS2\Processor(_Total)\% DPC Time","\\GORANS2\Processor(_Total)\% Interrupt Time","\\GORANS2\Processor(_Total)\DPCs Queued/sec","\\GORANS2\Processor(_Total)\DPC Rate","\\GORANS2\Processor(_Total)\% Idle Time","\\GORANS2\Processor(_Total)\% C1 Time","\\GORANS2\Processor(_Total)\% C2 Time","\\GORANS2\Processor(_Total)\% C3 Time","\\GORANS2\Processor(_Total)\C1 Transitions/sec","\\GORANS2\Processor(_Total)\C2 Transitions/sec","\\GORANS2\Processor(_Total)\C3 Transitions/sec"
	// "12/29/2020 18:29:30.669","9.444892","3.267452","6.223719","3363.882266","1.400337","2.645080","335.462196","4.000000","90.005169","0.000000","90.005169","0.000000","0.000000","2847.893813","0.000000","2.443209","1.089151","1.400337","2712.474227","0.000000","0.000000","15.433850","0.000000","96.079540","0.000000","96.079540","0.000000","0.000000","2578.448666","0.000000","15.357425","12.914216","2.489487","3988.106815","0.000000","0.000000","24.395440","0.000000","80.575353","1.314709","79.260643","0.000000","86.429559","3396.741430","0.000000","5.088289","3.578638","1.555930","2790.738782","0.000000","0.155593","14.637264","0.000000","93.201219","0.000000","93.201219","0.000000","0.000000","2619.871128","0.000000","13.334717","7.935241","5.445754","4082.402659","0.000000","0.155593","25.789465","0.000000","83.971794","0.695648","83.276146","0.000000","17.823607","3600.368676","0.000000","1.354058","1.089151","0.311186","2588.903855","0.000000","0.000000","9.260310","0.000000","97.035443","0.000000","97.035443","0.000000","0.000000","2528.761627","0.000000","6.955405","4.512196","2.489487","3180.269239","0.000000","0.155593","18.918913","0.000000","91.186061","0.342079","90.843982","0.000000","10.256042","2964.693206","0.000000","4.932696","4.045417","0.933558","2915.504033","0.000000","0.000000","12.446653","0.000000","93.565632","0.494346","93.071285","0.000000","81.251752","2715.162704","0.000000","6.799812","5.290161","1.555930","2831.762951","0.000000","0.000000","10.355615","0.000000","91.728349","0.000000","91.728349","0.000000","0.000000","2676.926586","0.000000","7.577777","2.178302","5.445754","1576.542874","0.000000","0.000000","26.984344","0.000000","90.383889","0.806888","89.577001","0.000000","259.388252","1135.533060","0.000000","8.355742","4.356603","4.045417","2713.768679","0.000000","0.000000","14.238971","0.000000","90.968388","0.000000","90.968388","0.000000","0.000000","2389.757404","0.000000","3.532360","2.489487","1.089151","2662.687614","0.000000","0.000000","9.658603","0.000000","94.798860","0.493655","94.305205","0.000000","5.675674","2562.815670","0.000000","7.098033","4.395502","2.748809","35407.043993","0.116694","0.259322","517.581624","4.000000","91.124975","0.345611","90.779364","0.000000","460.824886","32016.973969","0.000000"
	// "12/29/2020 18:29:40.702","8.410285","2.024943","6.386358","1141.133137","1.401883","3.738356","298.965518","5.000000","90.776654","0.000000","90.776654","0.000000","0.000000","904.672248","0.000000","1.089338","0.623059","0.467294","183.925102","0.000000","0.000000","7.476630","0.000000","97.837564","0.000000","97.837564","0.000000","0.000000","135.376850","0.000000","12.927464","10.747773","2.180708","1480.472458","0.000000","0.000000","21.931448","0.000000","81.874007","0.000000","81.874007","0.000000","0.000000","1206.628417","0.000000","3.893105","2.959532","0.934589","348.012211","0.000000","0.000000","26.616803","0.000000","94.244193","0.000000","94.244193","0.000000","0.000000","273.146221","0.000000","9.344873","6.542123","2.803767","1571.487969","0.000000","0.155765","25.021789","0.000000","85.592249","0.000000","85.592249","0.000000","0.000000","1300.235827","0.000000","0.933573","0.778824","0.155765","149.133849","0.000000","0.000000","9.171333","0.000000","97.226127","0.000000","97.226127","0.000000","0.000000","114.342597","0.000000","4.204634","2.803767","1.401883","700.011959","0.000000","0.155765","13.657311","0.000000","92.854395","0.000000","92.854395","0.000000","0.000000","622.753447","0.000000","4.516164","3.426826","1.090354","472.622714","0.000000","0.155765","10.766347","0.000000","94.835457","0.000000","94.835457","0.000000","0.000000","410.915593","0.000000","5.606518","3.894121","1.713413","475.114924","0.000000","0.155765","10.866036","0.000000","93.732629","0.492114","93.240515","0.000000","6.280369","380.311254","0.000000","5.294988","3.738356","1.557648","289.893873","0.000000","0.000000","10.267905","0.000000","94.541082","0.000000","94.541082","0.000000","0.000000","223.102644","0.000000","5.139223","3.582591","1.557648","624.149085","0.000000","0.000000","11.663543","0.000000","93.457389","0.000000","93.457389","0.000000","0.000000","553.868761","0.000000","2.335456","1.869178","0.467294","300.361155","0.000000","0.155765","9.570087","0.000000","96.106534","0.000000","96.106534","0.000000","0.000000","257.893896","0.000000","5.307968","3.582591","1.726394","7736.318435","0.116824","0.376431","455.974751","5.000000","92.756523","0.041009","92.715515","0.000000","6.280369","6383.247755","0.000000"
	// "12/29/2020 18:29:50.740","8.162735","2.179189","5.914942","1206.967704","0.622626","4.202722","311.430544","5.000000","90.643643","0.000000","90.643643","0.000000","0.000000","956.807084","0.000000","1.313854","0.933938","0.311313","268.691036","0.000000","0.000000","11.158100","0.000000","97.384273","0.000000","97.384273","0.000000","0.000000","206.125974","0.000000","12.676770","10.740290","1.867877","1517.601240","0.000000","0.155656","24.209092","0.000000","82.015927","0.876059","81.139868","0.000000","39.949983","1201.986409","0.000000","4.582638","3.113128","1.400907","451.106047","0.000000","0.000000","14.545380","0.000000","93.204245","0.000000","93.204245","0.000000","0.000000","353.074168","0.000000","8.941017","5.292317","3.580097","1619.219652","0.000000","0.000000","27.397121","0.000000","85.090487","0.000000","85.090487","0.000000","0.000000","1346.942084","0.000000","1.780824","1.556564","0.155656","214.793427","0.000000","0.000000","8.767079","0.000000","96.980336","0.000000","96.980336","0.000000","0.000000","164.482351","0.000000","5.983546","4.669691","1.245251","722.387356","0.000000","0.155656","20.821812","0.000000","91.380352","0.716233","90.664120","0.000000","38.754473","585.601004","0.000000","4.426982","3.268784","1.089595","471.330104","0.000000","0.155656","12.453237","0.000000","94.113678","0.000000","94.113678","0.000000","0.000000","397.308065","0.000000","5.049608","3.891410","1.089595","557.506502","0.000000","0.000000","12.552863","0.000000","92.579526","0.465431","92.114095","0.000000","9.464460","435.165904","0.000000","5.049608","3.735753","1.245251","403.684122","0.000000","0.000000","18.032287","0.000000","93.584534","0.000000","93.584534","0.000000","0.000000","314.020817","0.000000","5.360920","2.957471","2.334846","717.306435","0.000000","0.000000","15.442014","0.000000","93.111523","0.000000","93.111523","0.000000","0.000000","632.823677","0.000000","2.714762","1.712220","0.933938","407.171028","0.000000","0.000000","21.618819","0.000000","95.650813","0.501496","95.149317","0.000000","5.778302","336.835147","0.000000","5.503606","3.670895","1.764105","8557.764652","0.051885","0.389141","498.428347","5.000000","92.144944","0.213268","91.931676","0.000000","93.947218","6931.172684","0.000000"

	// C:\Users\goran>typeperf -si 10 "\PhysicalDisk(*)\*"
	// "(PDH-CSV 4.0)","\\GORANS2\PhysicalDisk(0 C:)\Current Disk Queue Length","\\GORANS2\PhysicalDisk(0 C:)\% Disk Time","\\GORANS2\PhysicalDisk(0 C:)\Avg. Disk Queue Length","\\GORANS2\PhysicalDisk(0 C:)\% Disk Read Time","\\GORANS2\PhysicalDisk(0 C:)\Avg. Disk Read Queue Length","\\GORANS2\PhysicalDisk(0 C:)\% Disk Write Time","\\GORANS2\PhysicalDisk(0 C:)\Avg. Disk Write Queue Length","\\GORANS2\PhysicalDisk(0 C:)\Avg. Disk sec/Transfer","\\GORANS2\PhysicalDisk(0 C:)\Avg. Disk sec/Read","\\GORANS2\PhysicalDisk(0 C:)\Avg. Disk sec/Write","\\GORANS2\PhysicalDisk(0 C:)\Disk Transfers/sec","\\GORANS2\PhysicalDisk(0 C:)\Disk Reads/sec","\\GORANS2\PhysicalDisk(0 C:)\Disk Writes/sec","\\GORANS2\PhysicalDisk(0 C:)\Disk Bytes/sec","\\GORANS2\PhysicalDisk(0 C:)\Disk Read Bytes/sec","\\GORANS2\PhysicalDisk(0 C:)\Disk Write Bytes/sec","\\GORANS2\PhysicalDisk(0 C:)\Avg. Disk Bytes/Transfer","\\GORANS2\PhysicalDisk(0 C:)\Avg. Disk Bytes/Read","\\GORANS2\PhysicalDisk(0 C:)\Avg. Disk Bytes/Write","\\GORANS2\PhysicalDisk(0 C:)\% Idle Time","\\GORANS2\PhysicalDisk(0 C:)\Split IO/Sec","\\GORANS2\PhysicalDisk(_Total)\Current Disk Queue Length","\\GORANS2\PhysicalDisk(_Total)\% Disk Time","\\GORANS2\PhysicalDisk(_Total)\Avg. Disk Queue Length","\\GORANS2\PhysicalDisk(_Total)\% Disk Read Time","\\GORANS2\PhysicalDisk(_Total)\Avg. Disk Read Queue Length","\\GORANS2\PhysicalDisk(_Total)\% Disk Write Time","\\GORANS2\PhysicalDisk(_Total)\Avg. Disk Write Queue Length","\\GORANS2\PhysicalDisk(_Total)\Avg. Disk sec/Transfer","\\GORANS2\PhysicalDisk(_Total)\Avg. Disk sec/Read","\\GORANS2\PhysicalDisk(_Total)\Avg. Disk sec/Write","\\GORANS2\PhysicalDisk(_Total)\Disk Transfers/sec","\\GORANS2\PhysicalDisk(_Total)\Disk Reads/sec","\\GORANS2\PhysicalDisk(_Total)\Disk Writes/sec","\\GORANS2\PhysicalDisk(_Total)\Disk Bytes/sec","\\GORANS2\PhysicalDisk(_Total)\Disk Read Bytes/sec","\\GORANS2\PhysicalDisk(_Total)\Disk Write Bytes/sec","\\GORANS2\PhysicalDisk(_Total)\Avg. Disk Bytes/Transfer","\\GORANS2\PhysicalDisk(_Total)\Avg. Disk Bytes/Read","\\GORANS2\PhysicalDisk(_Total)\Avg. Disk Bytes/Write","\\GORANS2\PhysicalDisk(_Total)\% Idle Time","\\GORANS2\PhysicalDisk(_Total)\Split IO/Sec"
	// "12/29/2020 18:34:28.308","0.000000","0.342941","0.003429","0.008645","0.000086","0.334296","0.003343","0.000201","0.000433","0.000198","17.066449","0.199608","16.866842","187688.624809","3270.370811","184418.253999","10997.520468","16384.000000","10933.775148","99.581332","0.399215","0.000000","0.342941","0.003429","0.008645","0.000086","0.334296","0.003343","0.000201","0.000433","0.000198","17.066449","0.199608","16.866842","187688.624809","3270.370811","184418.253999","10997.520468","16384.000000","10933.775148","99.581332","0.399215"
	// "12/29/2020 18:34:38.338","0.000000","0.414665","0.004147","0.005984","0.000060","0.408681","0.004087","0.000356","0.000200","0.000360","11.662307","0.299034","11.363274","116564.064942","4899.365252","111664.699691","9994.940171","16384.000000","9826.807018","99.559611","0.199356","0.000000","0.414665","0.004147","0.005984","0.000060","0.408681","0.004087","0.000356","0.000200","0.000360","11.662307","0.299034","11.363274","116564.064942","4899.365252","111664.699691","9994.940171","16384.000000","9826.807018","99.559611","0.199356"
	// "12/29/2020 18:34:48.369","0.000000","0.437254","0.004373","0.011335","0.000113","0.425918","0.004259","0.000199","0.000379","0.000197","21.931478","0.299066","21.632413","223710.647402","4899.890977","218810.756425","10200.436364","16384.000000","10114.949309","99.501144","0.299066","0.000000","0.437254","0.004373","0.011335","0.000113","0.425918","0.004259","0.000199","0.000379","0.000197","21.931478","0.299066","21.632413","223710.647402","4899.890977","218810.756425","10200.436364","16384.000000","10114.949309","99.501144","0.299066"
	// "12/29/2020 18:34:58.401","0.000000","0.547850","0.005478","0.022844","0.000228","0.525006","0.005250","0.000377","0.001146","0.000366","14.548399","0.199293","14.349106","226014.363216","3265.218791","222749.144425","15535.342466","16384.000000","15523.555556","99.450443","0.697526","0.000000","0.547850","0.005478","0.022844","0.000228","0.525006","0.005250","0.000377","0.001146","0.000366","14.548399","0.199293","14.349106","226014.363216","3265.218791","222749.144425","15535.342466","16384.000000","15523.555556","99.450443","0.697526"

	private LinkedHashMap<String, Integer> _instanceNameToRowId = new LinkedHashMap<>();
	private LinkedHashMap<String, Integer> _counterNameToColId  = new LinkedHashMap<>();
//	private List<List<Object>> _rows  = new ArrayList<>();

	/** Used to rewrite some counter names into "another" Group/Instance or CounterName */
	private Map<CounterColumnRewrite, CounterColumnRewrite> _counterRewriteRules;

	/** 
	 * Used to rewrite some counter names into "another" Group/Instance or CounterName 
	 * <p>
	 * TODO: the ReWrite Rule engine is <i>thin</i> for the moment, if it's really <b>used a lot</b> it has to be enhanced to handle wild-cards etc...
	 * <p>
	 * Example<br>
	 * <pre>
	 * Map<CounterColumn, CounterColumn> rewriteRules = new HashMap<>();
	 * 
	 * // if we see "Paging File(_Total)\% Usage" make it into "Memory\Paging File(_Total) - % Usage"
	 * rewriteRules.put(new CounterColumnRewrite("Paging File(_Total)", "% Usage")     , new CounterColumnRewrite("Memory", "Paging File(_Total) - % Usage"));
	 * 
	 * // if we see "Paging File(_Total)\% Usage Peak" make it into "Memory\Paging File(_Total) - % Usage Peak"
	 * rewriteRules.put(new CounterColumnRewrite("Paging File(_Total)", "% Usage Peak"), new CounterColumnRewrite("Memory", "Paging File(_Total) - % Usage Peak"));
	 *
	 * setCounterRewriteRules(rewriteRules);
	 * </pre>
	 */
	public void setCounterRewriteRules(Map<CounterColumnRewrite, CounterColumnRewrite> counterRewriteRules)
	{
		_counterRewriteRules = counterRewriteRules;
	}
	
	/**
	 * Check if we need to "re-transform" this counter into "another counter"
	 * 
	 * @param fullInstanceName    Full counter instance we potentially want to transform
	 * @param counterName         Counter Name we potentially want to transform
	 * 
	 * @return null if no rewrite rule exists. Otherwise a CounterColumn object (<br>without the _pos member set</br>)
	 */
	private CounterColumnRewrite hasCounterRewriteRule(String fullInstanceName, String counterName)
	{
		if (_counterRewriteRules == null || (_counterRewriteRules != null && _counterRewriteRules.isEmpty()) )
			return null;
		
		if (StringUtil.isNullOrBlank(fullInstanceName) || StringUtil.isNullOrBlank(counterName))
			return null;

		for (Entry<CounterColumnRewrite, CounterColumnRewrite> entry : _counterRewriteRules.entrySet())
		{
			CounterColumnRewrite from = entry.getKey();
			CounterColumnRewrite   to = entry.getValue();

			if (fullInstanceName.equals(from._fullInstanceName) && counterName.equals(from._counterName))
			{
				if (StringUtil.hasValue(to._fullInstanceName) && StringUtil.hasValue(to._counterName))
					return to;
			}
		}
		
		return null;
	}

	public void readHeader(String header)
	{
		LinkedHashSet<String> instanceNameSet = new LinkedHashSet<>();
//		LinkedHashSet<String> counterNameSet  = new LinkedHashSet<>();
		LinkedHashMap<String, List<String>> instCntrNameMap = new LinkedHashMap<>();

		if (_logger.isDebugEnabled())
			_logger.debug("readHeader(): |" + header + "|.");
		
		String[] sa = header.split(",");
		for (int i=0; i<sa.length; i++)
		{
			String colName = sa[i].trim();
			
			// Remove quotes around the column name
			if (colName.startsWith("\"") && colName.endsWith("\"")) 
			{
				colName = colName.substring(1, colName.length()-1);
			}

			if (i == 0 && colName.equals("(PDH-CSV 4.0)"))
			{
				// ADD
				_inputRowDict.put(i, new CounterColumn(i, "", "", "", "", COLNAME_TIMESTAMP));

				continue;
			}
			else
			{
				// Split the column name into it's "parts": \\hostname\group(instance)\counterName
//				String[] cns = colName.split("\\\\");
				String[] cns = colName.split("\\\\(?![^(]*\\))");
				// \\    // Match a '\'
				// (?!   // Negative look-ahead. We want to match a comma NOT followed by...
				// [^(]* // Any number of characters NOT '(', zero or more times
				// /)    // Followed by the ')' character
				// )     // Close the lookahead.

				// Renders in: [], [], [GORANS2], [PhysicalDisk(0 C:)], [Current Disk Queue Length]
				//              0   1          2                     3                            4

				if (cns.length == 5)
				{
					String hostname         = cns[2];
					String fullInstanceName = cns[3];
					String counterName      = cns[4];

					// If we have a "rewrite rule" to transform any Counter into "something else"
					CounterColumnRewrite rewriteRule = hasCounterRewriteRule(fullInstanceName, counterName);
					if (rewriteRule != null)
					{
						if (_logger.isDebugEnabled())
							_logger.debug("ReWrite rule EXISTS for fullInstanceName='" + fullInstanceName + "', counterName='" + counterName + "', TRANSFORMED TO: fullInstanceName='" + rewriteRule._fullInstanceName + "', counterName='" + rewriteRule._counterName + "'.");

						fullInstanceName = rewriteRule._fullInstanceName;
						counterName      = rewriteRule._counterName;
					}
					
					String counterGroup = fullInstanceName;
					String counterInstance = "";
					if (counterGroup.endsWith(")"))
					{
						int ciStartPos  = counterGroup.indexOf('(');
						counterInstance = counterGroup.substring(ciStartPos+1, counterGroup.length()-1);
						counterGroup    = counterGroup.substring(0, ciStartPos);
					}

					if (_logger.isDebugEnabled())
						_logger.debug("ADD: i=" + i + ", hostname='" + hostname + "', fullInstanceName='" + fullInstanceName + "', counterGroup='" + counterGroup + "', counterInstance='" + counterInstance + "', counterName='" + counterName + "'.");

					// ADD
					_inputRowDict.put(i, new CounterColumn(i, hostname, fullInstanceName, counterGroup, counterInstance, counterName));

					_counterGroupSet.add(counterGroup);

					instanceNameSet.add(fullInstanceName);
//					counterNameSet .add(counterName);

					// Header record some times "incomplete" or it do not hold ALL counters (when reading a row, the "entry count" in header/row is different)
					// This is an attempt to verify/correct the header record
					// Keep a list of counters for each "instance"
					// and at the end: verify that all instances has same number of counters 
					List<String> cntrList = instCntrNameMap.get(fullInstanceName);
					if (cntrList == null)
					{
						cntrList = new ArrayList<>();
						instCntrNameMap.put(fullInstanceName, cntrList);
					}
					if ( ! cntrList.contains(counterName) )
						cntrList.add(counterName);
				}
				else
				{
					throw new RuntimeException("Header column '" + colName + "'. should be of length 5, current length is " + cns.length + ".");
				}
			}
		}

		boolean isInputDictionaryValid = true;
		// Check that all counter instances has the same number of counter names
//		LinkedHashSet<String> finalCounterNameSet  = new LinkedHashSet<>();
//		List<String> finalCounterNameList;
		String       maxInstanceName  = null;
		int          maxInstCntrCount = -1;
		List<String> maxInstCntrList  = null;
		for (Entry<String, List<String>> entry : instCntrNameMap.entrySet())
		{
			String       key  = entry.getKey();
			List<String> list = entry.getValue();
			int instCntrCount = list.size();
			
			if (instCntrCount > maxInstCntrCount)
			{
				maxInstanceName  = key;
				maxInstCntrCount = instCntrCount;
				maxInstCntrList  = list;
			}
		}
		for (Entry<String, List<String>> entry : instCntrNameMap.entrySet())
		{
			String       key  = entry.getKey();
			List<String> list = entry.getValue();
			int instCntrCount = list.size();
			
			if (instCntrCount != maxInstCntrCount)
			{
				_logger.warn("Header Instance/CounterName count is not equaly distributed over all instances. The max instance named '" + maxInstanceName + "' has " + maxInstCntrCount + " entries. "
					+ "This instance '" + key + "' has " + instCntrCount + " entries. \n    Max-Entries=" + maxInstCntrList + "\n    ThisEntries=" + list);

				isInputDictionaryValid = false;
			}
		}

		// Did we see a error (miss-match)
		// Then lets try to "correct" the "input dictionary"
		if (isInputDictionaryValid == false)
		{
//			_inputRowDict.clear();
//			throw new RuntimeException("xxxx");
			LinkedHashMap<Integer, CounterColumn> newInputRowDict = new LinkedHashMap<>();
			
			// copy hostname from second pos(1)
			String hostname = _inputRowDict.get(1)._hostname;
			
			int pos = 0;
			newInputRowDict.put(pos, new CounterColumn(pos, "", "", "", "", COLNAME_TIMESTAMP));
			pos++;

			for (String fullInstanceName : instCntrNameMap.keySet())
			{
				String counterGroup = fullInstanceName;
				String counterInstance = "";
				if (counterGroup.endsWith(")"))
				{
					int ciStartPos  = counterGroup.indexOf('(');
					counterInstance = counterGroup.substring(ciStartPos+1, counterGroup.length()-1);
					counterGroup    = counterGroup.substring(0, ciStartPos);
				}

				for (String counterName : maxInstCntrList)
				{
					if (_logger.isDebugEnabled())
						_logger.debug("FIX ADD: pos=" + pos + ", hostname='" + hostname + "', fullInstanceName='" + fullInstanceName + "', counterGroup='" + counterGroup + "', counterInstance='" + counterInstance + "', counterName='" + counterName + "'.");

					newInputRowDict.put(pos, new CounterColumn(pos, hostname, fullInstanceName, counterGroup, counterInstance, counterName));
					pos++;
				}
			}
			
			// Finally start to use the NEW/FIXED dictionary
			_inputRowDict = newInputRowDict;
		}
		
		
		int instRowId = 0;
		for (String name : instanceNameSet)
		{
			_instanceNameToRowId.put(name, instRowId++);
		}
		
		int counterColId = 0;
		_counterNameToColId.put(COLNAME_TIMESTAMP, counterColId++);
		_counterNameToColId.put(COLNAME_HOSTNAME , counterColId++);
		_counterNameToColId.put(COLNAME_GROUP    , counterColId++);
		_counterNameToColId.put(COLNAME_INSTANCE , counterColId++);
//		for (String name : counterNameSet)
//		{
//			_counterNameToColId.put(name, counterColId++);
//		}
		for (String name : maxInstCntrList)
		{
			_counterNameToColId.put(name, counterColId++);
		}

		if (_logger.isDebugEnabled())
		{
			_logger.debug("readHeader(): _instanceNameToRowId="+_instanceNameToRowId);
			_logger.debug("readHeader(): _counterNameToColId ="+_counterNameToColId);
		}
	}

	/**
	 * Get a Map of column names and the Array position
	 * @return
	 */
	public Set<String> getCounterGroups()
	{
		return _counterGroupSet;
	}

	/**
	 * Get a Map of column names and the Array position
	 * @return
	 */
	public Map<String, Integer> getCounterNames()
	{
		return _counterNameToColId;
	}
	
	public int getExpectedRowCount()
	{
		return _instanceNameToRowId.size();
	}
	
	public static final String COLNAME_TIMESTAMP = "Timestamp";
	public static final String COLNAME_HOSTNAME  = "Hostname";
	public static final String COLNAME_GROUP     = "Group";
	public static final String COLNAME_INSTANCE  = "Instance";

	/**
	 * Translate the input line into multiple output lines (one for each instance)
	 * 
	 * @param inputRow
	 * @return
	 */
	public Object[][] readRowToObjectRows(String inputRow)
	{
		if (_inputRowDict.isEmpty())
			throw new RuntimeException("No headers has been assigned");

		Timestamp ts = null;

		// Allocate ALL rows we need
		Object[][] rows  = new Object[_instanceNameToRowId.size()][_counterNameToColId.size()];

		// Read the row, and put it into a destination row-id and col-id
		String[] sa = inputRow.split(",");

		// Check input
		if (sa.length != _inputRowDict.size())
		{
			throw new RuntimeException("Missmatch in entry count. inputRow[].lenth=" + sa.length + ", _inputRowDict.size()=" + _inputRowDict.size());
		}

		// Loop the input and put values in the desired output "slots"
		for (int i=0; i<sa.length; i++)
		{
			String val = sa[i].trim();

			// Remove quotes around the value
			if (val.startsWith("\"") && val.endsWith("\"")) 
			{
				val = val.substring(1, val.length()-1);
			}
			
			if (i == 0)
			{
				try
				{
					ts = new Timestamp(_sdf.parse(val).getTime());

					for (int r=0; r<rows.length; r++)
					{
						Object[] row = rows[r];
						row[0] = ts;
					}
				}
				catch(ParseException ex)
				{
					throw new RuntimeException("Problem parsing Counter Data Timestamp='" + val + "', inputRow=|" + inputRow + "|, Caught: " + ex, ex);
				}
				continue;
			}
			
			CounterColumn cc = _inputRowDict.get(i);
			if (cc == null)
				throw new RuntimeException("Column Header for position '" + i + "' could not be found. inputRow=|" + inputRow + "|");

			try
			{
				BigDecimal bd = new BigDecimal(val);
//				CounterValue counterValue = new CounterValue(cc, ts, bd);
				
				Integer rowId = _instanceNameToRowId.get(cc._fullInstanceName);
				Object[] row = rows[ rowId ];
				
				if (row[1] == null)
				{
					row[1] = cc._hostname;
					row[2] = cc._group;
					row[3] = cc._instance;
				}
				
				Integer colPos = _counterNameToColId.get(cc._name);
				row[colPos] = bd;
			}
			catch(NumberFormatException nfe)
			{
				throw new RuntimeException("Problem parsing Counter Data Value for position '" + i + "', val='" + val + "', inputRow=|" + inputRow + "|, Caught: " + nfe, nfe);
			}
		}
		
		return rows;
	}

	/**
	 * Translate the input line into multiple output lines (one for each instance)
	 * 
	 * @param inputRow
	 * @return
	 */
	public String[][] readRowToStringRows(String inputRow)
	{
		if (_inputRowDict.isEmpty())
			throw new RuntimeException("No headers has been assigned");

		Timestamp ts = null;

		// Allocate ALL rows we need
		String[][] rows  = new String[_instanceNameToRowId.size()][_counterNameToColId.size()];

		// Read the row, and put it into a destination row-id and col-id
		String[] sa = inputRow.split(",");

		// Check input
		if (sa.length != _inputRowDict.size())
		{
			throw new RuntimeException("Missmatch in entry count. inputRow[].lenth=" + sa.length + ", _inputRowDict.size()=" + _inputRowDict.size());
		}

		// Loop the input and put values in the desired output "slots"
		for (int i=0; i<sa.length; i++)
		{
			String val = sa[i].trim();

			// Remove quotes around the value
			if (val.startsWith("\"") && val.endsWith("\"")) 
			{
				val = val.substring(1, val.length()-1);
			}
			
			if (i == 0)
			{
				try
				{
					ts = new Timestamp(_sdf.parse(val).getTime());

					for (int r=0; r<rows.length; r++)
					{
						String[] row = rows[r];
						row[0] = ts.toString();
					}
				}
				catch(ParseException ex)
				{
					throw new RuntimeException("Problem parsing Counter Data Timestamp='" + val + "', inputRow=|" + inputRow + "|, Caught: " + ex, ex);
				}
				continue;
			}
			
			CounterColumn cc = _inputRowDict.get(i);
			if (cc == null)
				throw new RuntimeException("Column Header for position '" + i + "' could not be found. inputRow=|" + inputRow + "|");

			try
			{
//				BigDecimal bd = new BigDecimal(val);
//				CounterValue counterValue = new CounterValue(cc, ts, bd);
				
				Integer rowId = _instanceNameToRowId.get(cc._fullInstanceName);
				String[] row = rows[ rowId ];
				
				if (row[1] == null)
				{
					row[1] = cc._hostname;
					row[2] = cc._group;
					row[3] = cc._instance;
				}
				
				Integer colPos = _counterNameToColId.get(cc._name);
				row[colPos] = val;
			}
			catch(NumberFormatException nfe)
			{
				throw new RuntimeException("Problem parsing Counter Data Value for position '" + i + "', val='" + val + "', inputRow=|" + inputRow + "|, Caught: " + nfe, nfe);
			}
		}
		
		return rows;
	}
	
	public static class CounterColumn
	{
		private int    _pos; // starting at 0
		private String _hostname;
		private String _fullInstanceName;
		private String _group;
		private String _instance;
		private String _name;
		
		public CounterColumn(int pos, String hostname, String fullInstanceName, String group, String instance, String name)
		{
			_pos              = pos     ; // starting at 0
			_hostname         = hostname;
			_fullInstanceName = fullInstanceName;
			_group            = group   ;
			_instance         = instance;
			_name             = name    ;
		}
	}
	public static class CounterColumnRewrite
	{
		private String _fullInstanceName;
		private String _counterName;
		
		/** Constructor used for RE-WRITE Rules */
		public CounterColumnRewrite(String fullInstanceName, String counterName)
		{
			_fullInstanceName = fullInstanceName;
			_counterName      = counterName;
		}
	}
	

	
	
	/**
	 * Some manual testing...
	 * @param args
	 */
	public static void main(String[] args)
	{
		String cmd = "typeperf -si 10 \"\\PhysicalDisk(*)\\*\"";
		
		WindowsTypePerfCsvReader csvr = new WindowsTypePerfCsvReader();
		try
		{
			ProcessBuilder processBuilder = new ProcessBuilder();
//			processBuilder.command("typeperf", "-si", "1", "\"\\PhysicalDisk(*)\\*\"");
			processBuilder.command("typeperf", "-si", "2", "\\Processor(*)\\*");
//			processBuilder.command("typeperf", "-si", "2", "\\Memory\\*");
//			processBuilder.command("typeperf", "-si", "2", "\\System\\Processor Queue Length");
//			processBuilder.command("typeperf", "-si", "2", "\\System\\*");
//			processBuilder.command("typeperf", "-si", "2", "\\Paging File(*)\\*");
			
			Process process = processBuilder.start();
			
			int rowCount = 0;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) 
			{
				String line;

				while ((line = reader.readLine()) != null) 
				{
					System.out.println("|" + line + "|.");
					
					if (StringUtil.isNullOrBlank(line))
						continue;

					if (line.startsWith("(PDH-CSV ", 1))
						csvr.readHeader(line);
					else
					{
//						List<List<Object>> rows = csvr.readRow(line);
//						for (int r=0; r<rows.size(); r++)
//						{
//							System.out.println("ROW["+r+"]: " + rows.get(r));
//						}
						Object[][] rows = csvr.readRowToObjectRows(line);
						for (int r=0; r<rows.length; r++)
						{
							System.out.println("ROW["+r+"]: " + StringUtil.toCommaStr(rows[r]));
						}
					}
//					if (rowCount > 10)
//						process.destroy();

					rowCount++;
				}
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
}
