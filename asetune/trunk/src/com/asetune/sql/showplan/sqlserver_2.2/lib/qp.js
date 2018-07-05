(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["QP"] = factory();
	else
		root["QP"] = factory();
})(this, function() {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};

/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {

/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;

/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			exports: {},
/******/ 			id: moduleId,
/******/ 			loaded: false
/******/ 		};

/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);

/******/ 		// Flag the module as loaded
/******/ 		module.loaded = true;

/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}


/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;

/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;

/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";

/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	    value: true
	});
	exports.showPlan = exports.drawLines = undefined;

	var _transform = __webpack_require__(1);

	var transform = _interopRequireWildcard(_transform);

	var _svgLines = __webpack_require__(2);

	var _tooltip = __webpack_require__(5);

	function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } else { var newObj = {}; if (obj != null) { for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) newObj[key] = obj[key]; } } newObj.default = obj; return newObj; } }

	var qpXslt = __webpack_require__(6);

	function showPlan(container, planXml, options) {
	    options = setDefaults(options, {
	        jsTooltips: true
	    });

	    transform.setContentsUsingXslt(container, planXml, qpXslt);
	    (0, _svgLines.drawSvgLines)(container);

	    if (options.jsTooltips) {
	        (0, _tooltip.initTooltip)(container);
	    }
	}

	function setDefaults(options, defaults) {
	    var ret = {};
	    for (var attr in defaults) {
	        if (defaults.hasOwnProperty(attr)) {
	            ret[attr] = defaults[attr];
	        }
	    }
	    for (var _attr in options) {
	        if (options.hasOwnProperty(_attr)) {
	            ret[_attr] = options[_attr];
	        }
	    }
	    return ret;
	}

	exports.drawLines = _svgLines.drawSvgLines;
	exports.showPlan = showPlan;

/***/ },
/* 1 */
/***/ function(module, exports) {

	"use strict";

	Object.defineProperty(exports, "__esModule", {
	    value: true
	});
	/*
	 * Sets the contents of a container by transforming XML via XSLT.
	 * @container {Element} Container to set the contens for.
	 * @xml {string} Input XML.
	 * @xslt {string} XSLT transform to use.
	 */
	function setContentsUsingXslt(container, xml, xslt) {
	    if (window.ActiveXObject || "ActiveXObject" in window) {
	        var xsltDoc = new ActiveXObject("Microsoft.xmlDOM");
	        xsltDoc.async = false;
	        xsltDoc.loadXML(xslt);

	        var xmlDoc = new ActiveXObject("Microsoft.xmlDOM");
	        xmlDoc.async = false;
	        xmlDoc.loadXML(xml);

	        var result = xmlDoc.transformNode(xsltDoc);
	        container.innerHTML = result;
	    } else if (document.implementation && document.implementation.createDocument) {
	        var parser = new DOMParser();
	        var xsltProcessor = new XSLTProcessor();
	        xsltProcessor.importStylesheet(parser.parseFromString(xslt, "text/xml"));
	        var _result = xsltProcessor.transformToFragment(parser.parseFromString(xml, "text/xml"), document);
	        container.innerHTML = '';
	        container.appendChild(_result);
	    }
	}

	exports.setContentsUsingXslt = setContentsUsingXslt;

/***/ },
/* 2 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	    value: true
	});
	exports.drawSvgLines = undefined;

	var _svgjs = __webpack_require__(3);

	var _svgjs2 = _interopRequireDefault(_svgjs);

	var _utils = __webpack_require__(4);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	function drawSvgLines(container) {
	    var root = container.querySelector(".qp-root");
	    var draw = (0, _svgjs2.default)(root);

	    var clientRect = root.getBoundingClientRect();

	    var nodes = root.querySelectorAll('.qp-node');
	    for (var i = 0; i < nodes.length; i++) {
	        var node = nodes[i];
	        var previousNode = findParent(node);
	        if (previousNode != null) {
	            drawArrowBetweenNodes(draw, clientRect, previousNode, node);
	        }
	    }
	}

	function findParent(node) {
	    var row = (0, _utils.findAncestor)(node, 'qp-tr');
	    var parentRow = (0, _utils.findAncestor)(row, 'qp-tr');
	    if (!parentRow) {
	        return null;
	    }
	    return parentRow.children[0].children[0];
	}

	/**
	 * Draws the arrow between two nodes.
	 * @draw SVG drawing context to use.
	 * @offset Bounding client rect of the root SVG context.
	 * @fromElement Node element from which to draw the arrow (leftmost node).
	 * @toElement Node element to which to draw the arrow (rightmost node).
	 */
	function drawArrowBetweenNodes(draw, offset, fromElement, toElement) {
	    var fromOffset = fromElement.getBoundingClientRect();
	    var toOffset = toElement.getBoundingClientRect();

	    var fromX = fromOffset.right;
	    var fromY = (fromOffset.top + fromOffset.bottom) / 2;

	    var toX = toOffset.left;
	    var toY = (toOffset.top + toOffset.bottom) / 2;

	    var midOffsetLeft = fromX / 2 + toX / 2;

	    var fromPoint = {
	        x: fromX - offset.left + 1,
	        y: fromY - offset.top
	    };
	    var toPoint = {
	        x: toOffset.left - offset.left - 1,
	        y: toY - offset.top
	    };
	    var bendOffsetX = midOffsetLeft - offset.left;
	    drawArrow(draw, fromPoint, toPoint, bendOffsetX);
	}

	/**
	 * Draws an arrow between two points.
	 * @draw SVG drawing context to use.
	 * @from {x,y} coordinates of tail end.
	 * @to {x,y} coordinates of the pointy end.
	 * @bendX Offset from toPoint at which the "bend" should happen. (X axis) 
	 */
	function drawArrow(draw, from, to, bendX) {

	    var points = [[from.x, from.y], [from.x + 3, from.y - 3], [from.x + 3, from.y - 1], [bendX + (from.y <= to.y ? 1 : -1), from.y - 1], [bendX + (from.y <= to.y ? 1 : -1), to.y - 1], [to.x, to.y - 1], [to.x, to.y + 1], [bendX + (from.y <= to.y ? -1 : 1), to.y + 1], [bendX + (from.y <= to.y ? -1 : 1), from.y + 1], [from.x + 3, from.y + 1], [from.x + 3, from.y + 3], [from.x, from.y]];

	    draw.polyline(points).fill('#E3E3E3').stroke({
	        color: '#505050',
	        width: 0.5
	    });
	}

	exports.drawSvgLines = drawSvgLines;

/***/ },
/* 3 */
/***/ function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_RESULT__;'use strict';var _typeof=typeof Symbol==="function"&&typeof Symbol.iterator==="symbol"?function(obj){return typeof obj;}:function(obj){return obj&&typeof Symbol==="function"&&obj.constructor===Symbol?"symbol":typeof obj;};/*!
	* svg.js - A lightweight library for manipulating and animating SVG.
	* @version 2.2.5
	* http://www.svgjs.com
	*
	* @copyright Wout Fierens <wout@impinc.co.uk>
	* @license MIT
	*
	* BUILT: Thu Jan 21 2016 16:57:48 GMT+0100 (Mitteleuropäische Zeit)
	*/;(function(root,factory){if(true){!(__WEBPACK_AMD_DEFINE_RESULT__ = function(){return factory(root,root.document);}.call(exports, __webpack_require__, exports, module), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));}else if((typeof exports==='undefined'?'undefined':_typeof(exports))==='object'){module.exports=root.document?factory(root,root.document):function(w){return factory(w,w.document);};}else{root.SVG=factory(root,root.document);}})(typeof window!=="undefined"?window:undefined,function(window,document){// The main wrapping element
	var SVG=this.SVG=function(element){if(SVG.supported){element=new SVG.Doc(element);if(!SVG.parser)SVG.prepare(element);return element;}};// Default namespaces
	SVG.ns='http://www.w3.org/2000/svg';SVG.xmlns='http://www.w3.org/2000/xmlns/';SVG.xlink='http://www.w3.org/1999/xlink';SVG.svgjs='http://svgjs.com/svgjs';// Svg support test
	SVG.supported=function(){return!!document.createElementNS&&!!document.createElementNS(SVG.ns,'svg').createSVGRect;}();// Don't bother to continue if SVG is not supported
	if(!SVG.supported)return false;// Element id sequence
	SVG.did=1000;// Get next named element id
	SVG.eid=function(name){return'Svgjs'+capitalize(name)+SVG.did++;};// Method for element creation
	SVG.create=function(name){// create element
	var element=document.createElementNS(this.ns,name);// apply unique id
	element.setAttribute('id',this.eid(name));return element;};// Method for extending objects
	SVG.extend=function(){var modules,methods,key,i;// Get list of modules
	modules=[].slice.call(arguments);// Get object with extensions
	methods=modules.pop();for(i=modules.length-1;i>=0;i--){if(modules[i])for(key in methods){modules[i].prototype[key]=methods[key];}}// Make sure SVG.Set inherits any newly added methods
	if(SVG.Set&&SVG.Set.inherit)SVG.Set.inherit();};// Invent new element
	SVG.invent=function(config){// Create element initializer
	var initializer=typeof config.create=='function'?config.create:function(){this.constructor.call(this,SVG.create(config.create));};// Inherit prototype
	if(config.inherit)initializer.prototype=new config.inherit();// Extend with methods
	if(config.extend)SVG.extend(initializer,config.extend);// Attach construct method to parent
	if(config.construct)SVG.extend(config.parent||SVG.Container,config.construct);return initializer;};// Adopt existing svg elements
	SVG.adopt=function(node){// check for presence of node
	if(!node)return null;// make sure a node isn't already adopted
	if(node.instance)return node.instance;// initialize variables
	var element;// adopt with element-specific settings
	if(node.nodeName=='svg')element=node.parentNode instanceof SVGElement?new SVG.Nested():new SVG.Doc();else if(node.nodeName=='linearGradient')element=new SVG.Gradient('linear');else if(node.nodeName=='radialGradient')element=new SVG.Gradient('radial');else if(SVG[capitalize(node.nodeName)])element=new SVG[capitalize(node.nodeName)]();else element=new SVG.Element(node);// ensure references
	element.type=node.nodeName;element.node=node;node.instance=element;// SVG.Class specific preparations
	if(element instanceof SVG.Doc)element.namespace().defs();// pull svgjs data from the dom (getAttributeNS doesn't work in html5)
	element.setData(JSON.parse(node.getAttribute('svgjs:data'))||{});return element;};// Initialize parsing element
	SVG.prepare=function(element){// Select document body and create invisible svg element
	var body=document.getElementsByTagName('body')[0],draw=(body?new SVG.Doc(body):element.nested()).size(2,0),path=SVG.create('path');// Insert parsers
	draw.node.appendChild(path);// Create parser object
	SVG.parser={body:body||element.parent(),draw:draw.style('opacity:0;position:fixed;left:100%;top:100%;overflow:hidden'),poly:draw.polyline().node,path:path};};// Storage for regular expressions
	SVG.regex={// Parse unit value
	unit:/^(-?[\d\.]+)([a-z%]{0,2})$/// Parse hex value
	,hex:/^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i// Parse rgb value
	,rgb:/rgb\((\d+),(\d+),(\d+)\)/// Parse reference id
	,reference:/#([a-z0-9\-_]+)/i// Parse matrix wrapper
	,matrix:/matrix\(|\)/g// Elements of a matrix
	,matrixElements:/,*\s+|,/// Whitespace
	,whitespace:/\s/g// Test hex value
	,isHex:/^#[a-f0-9]{3,6}$/i// Test rgb value
	,isRgb:/^rgb\(/// Test css declaration
	,isCss:/[^:]+:[^;]+;?/// Test for blank string
	,isBlank:/^(\s+)?$/// Test for numeric string
	,isNumber:/^[+-]?(\d+(\.\d*)?|\.\d+)(e[+-]?\d+)?$/i// Test for percent value
	,isPercent:/^-?[\d\.]+%$/// Test for image url
	,isImage:/\.(jpg|jpeg|png|gif|svg)(\?[^=]+.*)?/i// The following regex are used to parse the d attribute of a path
	// Replaces all negative exponents
	,negExp:/e\-/gi// Replaces all comma
	,comma:/,/g// Replaces all hyphens
	,hyphen:/\-/g// Replaces and tests for all path letters
	,pathLetters:/[MLHVCSQTAZ]/gi// yes we need this one, too
	,isPathLetter:/[MLHVCSQTAZ]/i// split at whitespaces
	,whitespaces:/\s+/// matches X
	,X:/X/g};SVG.utils={// Map function
	map:function map(array,block){var i,il=array.length,result=[];for(i=0;i<il;i++){result.push(block(array[i]));}return result;}// Degrees to radians
	,radians:function radians(d){return d%360*Math.PI/180;}// Radians to degrees
	,degrees:function degrees(r){return r*180/Math.PI%360;},filterSVGElements:function filterSVGElements(p){return[].filter.call(p,function(el){return el instanceof SVGElement;});}};SVG.defaults={// Default attribute values
	attrs:{// fill and stroke
	'fill-opacity':1,'stroke-opacity':1,'stroke-width':0,'stroke-linejoin':'miter','stroke-linecap':'butt',fill:'#000000',stroke:'#000000',opacity:1// position
	,x:0,y:0,cx:0,cy:0// size
	,width:0,height:0// radius
	,r:0,rx:0,ry:0// gradient
	,offset:0,'stop-opacity':1,'stop-color':'#000000'// text
	,'font-size':16,'font-family':'Helvetica, Arial, sans-serif','text-anchor':'start'}};// Module for color convertions
	SVG.Color=function(color){var match;// initialize defaults
	this.r=0;this.g=0;this.b=0;// parse color
	if(typeof color==='string'){if(SVG.regex.isRgb.test(color)){// get rgb values
	match=SVG.regex.rgb.exec(color.replace(/\s/g,''));// parse numeric values
	this.r=parseInt(match[1]);this.g=parseInt(match[2]);this.b=parseInt(match[3]);}else if(SVG.regex.isHex.test(color)){// get hex values
	match=SVG.regex.hex.exec(fullHex(color));// parse numeric values
	this.r=parseInt(match[1],16);this.g=parseInt(match[2],16);this.b=parseInt(match[3],16);}}else if((typeof color==='undefined'?'undefined':_typeof(color))==='object'){this.r=color.r;this.g=color.g;this.b=color.b;}};SVG.extend(SVG.Color,{// Default to hex conversion
	toString:function toString(){return this.toHex();}// Build hex value
	,toHex:function toHex(){return'#'+compToHex(this.r)+compToHex(this.g)+compToHex(this.b);}// Build rgb value
	,toRgb:function toRgb(){return'rgb('+[this.r,this.g,this.b].join()+')';}// Calculate true brightness
	,brightness:function brightness(){return this.r/255*0.30+this.g/255*0.59+this.b/255*0.11;}// Make color morphable
	,morph:function morph(color){this.destination=new SVG.Color(color);return this;}// Get morphed color at given position
	,at:function at(pos){// make sure a destination is defined
	if(!this.destination)return this;// normalise pos
	pos=pos<0?0:pos>1?1:pos;// generate morphed color
	return new SVG.Color({r:~~(this.r+(this.destination.r-this.r)*pos),g:~~(this.g+(this.destination.g-this.g)*pos),b:~~(this.b+(this.destination.b-this.b)*pos)});}});// Testers
	// Test if given value is a color string
	SVG.Color.test=function(color){color+='';return SVG.regex.isHex.test(color)||SVG.regex.isRgb.test(color);};// Test if given value is a rgb object
	SVG.Color.isRgb=function(color){return color&&typeof color.r=='number'&&typeof color.g=='number'&&typeof color.b=='number';};// Test if given value is a color
	SVG.Color.isColor=function(color){return SVG.Color.isRgb(color)||SVG.Color.test(color);};// Module for array conversion
	SVG.Array=function(array,fallback){array=(array||[]).valueOf();// if array is empty and fallback is provided, use fallback
	if(array.length==0&&fallback)array=fallback.valueOf();// parse array
	this.value=this.parse(array);};SVG.extend(SVG.Array,{// Make array morphable
	morph:function morph(array){this.destination=this.parse(array);// normalize length of arrays
	if(this.value.length!=this.destination.length){var lastValue=this.value[this.value.length-1],lastDestination=this.destination[this.destination.length-1];while(this.value.length>this.destination.length){this.destination.push(lastDestination);}while(this.value.length<this.destination.length){this.value.push(lastValue);}}return this;}// Clean up any duplicate points
	,settle:function settle(){// find all unique values
	for(var i=0,il=this.value.length,seen=[];i<il;i++){if(seen.indexOf(this.value[i])==-1)seen.push(this.value[i]);}// set new value
	return this.value=seen;}// Get morphed array at given position
	,at:function at(pos){// make sure a destination is defined
	if(!this.destination)return this;// generate morphed array
	for(var i=0,il=this.value.length,array=[];i<il;i++){array.push(this.value[i]+(this.destination[i]-this.value[i])*pos);}return new SVG.Array(array);}// Convert array to string
	,toString:function toString(){return this.value.join(' ');}// Real value
	,valueOf:function valueOf(){return this.value;}// Parse whitespace separated string
	,parse:function parse(array){array=array.valueOf();// if already is an array, no need to parse it
	if(Array.isArray(array))return array;return this.split(array);}// Strip unnecessary whitespace
	,split:function split(string){return string.trim().split(/\s+/);}// Reverse array
	,reverse:function reverse(){this.value.reverse();return this;}});// Poly points array
	SVG.PointArray=function(array,fallback){this.constructor.call(this,array,fallback||[[0,0]]);};// Inherit from SVG.Array
	SVG.PointArray.prototype=new SVG.Array();SVG.extend(SVG.PointArray,{// Convert array to string
	toString:function toString(){// convert to a poly point string
	for(var i=0,il=this.value.length,array=[];i<il;i++){array.push(this.value[i].join(','));}return array.join(' ');}// Convert array to line object
	,toLine:function toLine(){return{x1:this.value[0][0],y1:this.value[0][1],x2:this.value[1][0],y2:this.value[1][1]};}// Get morphed array at given position
	,at:function at(pos){// make sure a destination is defined
	if(!this.destination)return this;// generate morphed point string
	for(var i=0,il=this.value.length,array=[];i<il;i++){array.push([this.value[i][0]+(this.destination[i][0]-this.value[i][0])*pos,this.value[i][1]+(this.destination[i][1]-this.value[i][1])*pos]);}return new SVG.PointArray(array);}// Parse point string
	,parse:function parse(array){array=array.valueOf();// if already is an array, no need to parse it
	if(Array.isArray(array))return array;// split points
	array=this.split(array);// parse points
	for(var i=0,il=array.length,p,points=[];i<il;i++){p=array[i].split(',');points.push([parseFloat(p[0]),parseFloat(p[1])]);}return points;}// Move point string
	,move:function move(x,y){var box=this.bbox();// get relative offset
	x-=box.x;y-=box.y;// move every point
	if(!isNaN(x)&&!isNaN(y))for(var i=this.value.length-1;i>=0;i--){this.value[i]=[this.value[i][0]+x,this.value[i][1]+y];}return this;}// Resize poly string
	,size:function size(width,height){var i,box=this.bbox();// recalculate position of all points according to new size
	for(i=this.value.length-1;i>=0;i--){this.value[i][0]=(this.value[i][0]-box.x)*width/box.width+box.x;this.value[i][1]=(this.value[i][1]-box.y)*height/box.height+box.y;}return this;}// Get bounding box of points
	,bbox:function bbox(){SVG.parser.poly.setAttribute('points',this.toString());return SVG.parser.poly.getBBox();}});// Path points array
	SVG.PathArray=function(array,fallback){this.constructor.call(this,array,fallback||[['M',0,0]]);};// Inherit from SVG.Array
	SVG.PathArray.prototype=new SVG.Array();SVG.extend(SVG.PathArray,{// Convert array to string
	toString:function toString(){return arrayToString(this.value);}// Move path string
	,move:function move(x,y){// get bounding box of current situation
	var box=this.bbox();// get relative offset
	x-=box.x;y-=box.y;if(!isNaN(x)&&!isNaN(y)){// move every point
	for(var l,i=this.value.length-1;i>=0;i--){l=this.value[i][0];if(l=='M'||l=='L'||l=='T'){this.value[i][1]+=x;this.value[i][2]+=y;}else if(l=='H'){this.value[i][1]+=x;}else if(l=='V'){this.value[i][1]+=y;}else if(l=='C'||l=='S'||l=='Q'){this.value[i][1]+=x;this.value[i][2]+=y;this.value[i][3]+=x;this.value[i][4]+=y;if(l=='C'){this.value[i][5]+=x;this.value[i][6]+=y;}}else if(l=='A'){this.value[i][6]+=x;this.value[i][7]+=y;}}}return this;}// Resize path string
	,size:function size(width,height){// get bounding box of current situation
	var i,l,box=this.bbox();// recalculate position of all points according to new size
	for(i=this.value.length-1;i>=0;i--){l=this.value[i][0];if(l=='M'||l=='L'||l=='T'){this.value[i][1]=(this.value[i][1]-box.x)*width/box.width+box.x;this.value[i][2]=(this.value[i][2]-box.y)*height/box.height+box.y;}else if(l=='H'){this.value[i][1]=(this.value[i][1]-box.x)*width/box.width+box.x;}else if(l=='V'){this.value[i][1]=(this.value[i][1]-box.y)*height/box.height+box.y;}else if(l=='C'||l=='S'||l=='Q'){this.value[i][1]=(this.value[i][1]-box.x)*width/box.width+box.x;this.value[i][2]=(this.value[i][2]-box.y)*height/box.height+box.y;this.value[i][3]=(this.value[i][3]-box.x)*width/box.width+box.x;this.value[i][4]=(this.value[i][4]-box.y)*height/box.height+box.y;if(l=='C'){this.value[i][5]=(this.value[i][5]-box.x)*width/box.width+box.x;this.value[i][6]=(this.value[i][6]-box.y)*height/box.height+box.y;}}else if(l=='A'){// resize radii
	this.value[i][1]=this.value[i][1]*width/box.width;this.value[i][2]=this.value[i][2]*height/box.height;// move position values
	this.value[i][6]=(this.value[i][6]-box.x)*width/box.width+box.x;this.value[i][7]=(this.value[i][7]-box.y)*height/box.height+box.y;}}return this;}// Absolutize and parse path to array
	,parse:function parse(array){// if it's already a patharray, no need to parse it
	if(array instanceof SVG.PathArray)return array.valueOf();// prepare for parsing
	var i,x0,y0,s,seg,arr,x=0,y=0,paramCnt={'M':2,'L':2,'H':1,'V':1,'C':6,'S':4,'Q':4,'T':2,'A':7};if(typeof array=='string'){array=array.replace(SVG.regex.negExp,'X')// replace all negative exponents with certain char
	.replace(SVG.regex.pathLetters,' $& ')// put some room between letters and numbers
	.replace(SVG.regex.hyphen,' -')// add space before hyphen
	.replace(SVG.regex.comma,' ')// unify all spaces
	.replace(SVG.regex.X,'e-')// add back the expoent
	.trim()// trim
	.split(SVG.regex.whitespaces);// split into array
	// at this place there could be parts like ['3.124.854.32'] because we could not determine the point as seperator till now
	// we fix this elements in the next loop
	for(i=array.length;--i;){if(array[i].indexOf('.')!=array[i].lastIndexOf('.')){var split=array[i].split('.');// split at the point
	var first=[split.shift(),split.shift()].join('.');// join the first number together
	array.splice.apply(array,[i,1].concat(first,split.map(function(el){return'.'+el;})));// add first and all other entries back to array
	}}}else{array=array.reduce(function(prev,curr){return[].concat.apply(prev,curr);},[]);}// array now is an array containing all parts of a path e.g. ['M', '0', '0', 'L', '30', '30' ...]
	var arr=[];do{// Test if we have a path letter
	if(SVG.regex.isPathLetter.test(array[0])){s=array[0];array.shift();// If last letter was a move command and we got no new, it defaults to [L]ine
	}else if(s=='M'){s='L';}else if(s=='m'){s='l';}// add path letter as first element
	seg=[s.toUpperCase()];// push all necessary parameters to segment
	for(i=0;i<paramCnt[seg[0]];++i){seg.push(parseFloat(array.shift()));}// upper case
	if(s==seg[0]){if(s=='M'||s=='L'||s=='C'||s=='Q'){x=seg[paramCnt[seg[0]]-1];y=seg[paramCnt[seg[0]]];}else if(s=='V'){y=seg[1];}else if(s=='H'){x=seg[1];}else if(s=='A'){x=seg[6];y=seg[7];}// lower case
	}else{// convert relative to absolute values
	if(s=='m'||s=='l'||s=='c'||s=='s'||s=='q'||s=='t'){seg[1]+=x;seg[2]+=y;if(seg[3]!=null){seg[3]+=x;seg[4]+=y;}if(seg[5]!=null){seg[5]+=x;seg[6]+=y;}// move pointer
	x=seg[paramCnt[seg[0]]-1];y=seg[paramCnt[seg[0]]];}else if(s=='v'){seg[1]+=y;y=seg[1];}else if(s=='h'){seg[1]+=x;x=seg[1];}else if(s=='a'){seg[6]+=x;seg[7]+=y;x=seg[6];y=seg[7];}}if(seg[0]=='M'){x0=x;y0=y;}if(seg[0]=='Z'){x=x0;y=y0;}arr.push(seg);}while(array.length);return arr;}// Get bounding box of path
	,bbox:function bbox(){SVG.parser.path.setAttribute('d',this.toString());return SVG.parser.path.getBBox();}});// Module for unit convertions
	SVG.Number=SVG.invent({// Initialize
	create:function create(value,unit){// initialize defaults
	this.value=0;this.unit=unit||'';// parse value
	if(typeof value==='number'){// ensure a valid numeric value
	this.value=isNaN(value)?0:!isFinite(value)?value<0?-3.4e+38:+3.4e+38:value;}else if(typeof value==='string'){unit=value.match(SVG.regex.unit);if(unit){// make value numeric
	this.value=parseFloat(unit[1]);// normalize
	if(unit[2]=='%')this.value/=100;else if(unit[2]=='s')this.value*=1000;// store unit
	this.unit=unit[2];}}else{if(value instanceof SVG.Number){this.value=value.valueOf();this.unit=value.unit;}}}// Add methods
	,extend:{// Stringalize
	toString:function toString(){return(this.unit=='%'?~~(this.value*1e8)/1e6:this.unit=='s'?this.value/1e3:this.value)+this.unit;},toJSON:function toJSON(){return this.toString();},// Convert to primitive
	valueOf:function valueOf(){return this.value;}// Add number
	,plus:function plus(number){return new SVG.Number(this+new SVG.Number(number),this.unit);}// Subtract number
	,minus:function minus(number){return this.plus(-new SVG.Number(number));}// Multiply number
	,times:function times(number){return new SVG.Number(this*new SVG.Number(number),this.unit);}// Divide number
	,divide:function divide(number){return new SVG.Number(this/new SVG.Number(number),this.unit);}// Convert to different unit
	,to:function to(unit){var number=new SVG.Number(this);if(typeof unit==='string')number.unit=unit;return number;}// Make number morphable
	,morph:function morph(number){this.destination=new SVG.Number(number);return this;}// Get morphed number at given position
	,at:function at(pos){// Make sure a destination is defined
	if(!this.destination)return this;// Generate new morphed number
	return new SVG.Number(this.destination).minus(this).times(pos).plus(this);}}});SVG.ViewBox=function(element){var x,y,width,height,wm=1// width multiplier
	,hm=1// height multiplier
	,box=element.bbox(),view=(element.attr('viewBox')||'').match(/-?[\d\.]+/g),we=element,he=element;// get dimensions of current node
	width=new SVG.Number(element.width());height=new SVG.Number(element.height());// find nearest non-percentual dimensions
	while(width.unit=='%'){wm*=width.value;width=new SVG.Number(we instanceof SVG.Doc?we.parent().offsetWidth:we.parent().width());we=we.parent();}while(height.unit=='%'){hm*=height.value;height=new SVG.Number(he instanceof SVG.Doc?he.parent().offsetHeight:he.parent().height());he=he.parent();}// ensure defaults
	this.x=box.x;this.y=box.y;this.width=width*wm;this.height=height*hm;this.zoom=1;if(view){// get width and height from viewbox
	x=parseFloat(view[0]);y=parseFloat(view[1]);width=parseFloat(view[2]);height=parseFloat(view[3]);// calculate zoom accoring to viewbox
	this.zoom=this.width/this.height>width/height?this.height/height:this.width/width;// calculate real pixel dimensions on parent SVG.Doc element
	this.x=x;this.y=y;this.width=width;this.height=height;}};//
	SVG.extend(SVG.ViewBox,{// Parse viewbox to string
	toString:function toString(){return this.x+' '+this.y+' '+this.width+' '+this.height;}});SVG.Element=SVG.invent({// Initialize node
	create:function create(node){// make stroke value accessible dynamically
	this._stroke=SVG.defaults.attrs.stroke;// initialize data object
	this.dom={};// create circular reference
	if(this.node=node){this.type=node.nodeName;this.node.instance=this;// store current attribute value
	this._stroke=node.getAttribute('stroke')||this._stroke;}}// Add class methods
	,extend:{// Move over x-axis
	x:function x(_x){return this.attr('x',_x);}// Move over y-axis
	,y:function y(_y){return this.attr('y',_y);}// Move by center over x-axis
	,cx:function cx(x){return x==null?this.x()+this.width()/2:this.x(x-this.width()/2);}// Move by center over y-axis
	,cy:function cy(y){return y==null?this.y()+this.height()/2:this.y(y-this.height()/2);}// Move element to given x and y values
	,move:function move(x,y){return this.x(x).y(y);}// Move element by its center
	,center:function center(x,y){return this.cx(x).cy(y);}// Set width of element
	,width:function width(_width){return this.attr('width',_width);}// Set height of element
	,height:function height(_height){return this.attr('height',_height);}// Set element size to given width and height
	,size:function size(width,height){var p=proportionalSize(this.bbox(),width,height);return this.width(new SVG.Number(p.width)).height(new SVG.Number(p.height));}// Clone element
	,clone:function clone(){// clone element and assign new id
	var clone=assignNewId(this.node.cloneNode(true));// insert the clone after myself
	this.after(clone);return clone;}// Remove element
	,remove:function remove(){if(this.parent())this.parent().removeElement(this);return this;}// Replace element
	,replace:function replace(element){this.after(element).remove();return element;}// Add element to given container and return self
	,addTo:function addTo(parent){return parent.put(this);}// Add element to given container and return container
	,putIn:function putIn(parent){return parent.add(this);}// Get / set id
	,id:function id(_id){return this.attr('id',_id);}// Checks whether the given point inside the bounding box of the element
	,inside:function inside(x,y){var box=this.bbox();return x>box.x&&y>box.y&&x<box.x+box.width&&y<box.y+box.height;}// Show element
	,show:function show(){return this.style('display','');}// Hide element
	,hide:function hide(){return this.style('display','none');}// Is element visible?
	,visible:function visible(){return this.style('display')!='none';}// Return id on string conversion
	,toString:function toString(){return this.attr('id');}// Return array of classes on the node
	,classes:function classes(){var attr=this.attr('class');return attr==null?[]:attr.trim().split(/\s+/);}// Return true if class exists on the node, false otherwise
	,hasClass:function hasClass(name){return this.classes().indexOf(name)!=-1;}// Add class to the node
	,addClass:function addClass(name){if(!this.hasClass(name)){var array=this.classes();array.push(name);this.attr('class',array.join(' '));}return this;}// Remove class from the node
	,removeClass:function removeClass(name){if(this.hasClass(name)){this.attr('class',this.classes().filter(function(c){return c!=name;}).join(' '));}return this;}// Toggle the presence of a class on the node
	,toggleClass:function toggleClass(name){return this.hasClass(name)?this.removeClass(name):this.addClass(name);}// Get referenced element form attribute value
	,reference:function reference(attr){return SVG.get(this.attr(attr));}// Returns the parent element instance
	,parent:function parent(type){var parent=this;// check for parent
	if(!parent.node.parentNode)return null;// get parent element
	parent=SVG.adopt(parent.node.parentNode);if(!type)return parent;// loop trough ancestors if type is given
	while(parent.node instanceof SVGElement){if(typeof type==='string'?parent.matches(type):parent instanceof type)return parent;parent=SVG.adopt(parent.node.parentNode);}}// Get parent document
	,doc:function doc(){return this instanceof SVG.Doc?this:this.parent(SVG.Doc);}// return array of all ancestors of given type up to the root svg
	,parents:function parents(type){var parents=[],parent=this;do{parent=parent.parent(type);if(!parent||!parent.node)break;parents.push(parent);}while(parent.parent);return parents;}// matches the element vs a css selector
	,matches:function matches(selector){return _matches(this.node,selector);}// Returns the svg node to call native svg methods on it
	,native:function native(){return this.node;}// Import raw svg
	,svg:function svg(_svg){// create temporary holder
	var well=document.createElement('svg');// act as a setter if svg is given
	if(_svg&&this instanceof SVG.Parent){// dump raw svg
	well.innerHTML='<svg>'+_svg.replace(/\n/,'').replace(/<(\w+)([^<]+?)\/>/g,'<$1$2></$1>')+'</svg>';// transplant nodes
	for(var i=0,il=well.firstChild.childNodes.length;i<il;i++){this.node.appendChild(well.firstChild.firstChild);}// otherwise act as a getter
	}else{// create a wrapping svg element in case of partial content
	well.appendChild(_svg=document.createElement('svg'));// write svgjs data to the dom
	this.writeDataToDom();// insert a copy of this node
	_svg.appendChild(this.node.cloneNode(true));// return target element
	return well.innerHTML.replace(/^<svg>/,'').replace(/<\/svg>$/,'');}return this;}// write svgjs data to the dom
	,writeDataToDom:function writeDataToDom(){// dump variables recursively
	if(this.each||this.lines){var fn=this.each?this:this.lines();fn.each(function(){this.writeDataToDom();});}// remove previously set data
	this.node.removeAttribute('svgjs:data');if(Object.keys(this.dom).length)this.node.setAttribute('svgjs:data',JSON.stringify(this.dom));// see #428
	return this;}// set given data to the elements data property
	,setData:function setData(o){this.dom=o;return this;}}});SVG.FX=SVG.invent({// Initialize FX object
	create:function create(element){// store target element
	this.target=element;}// Add class methods
	,extend:{// Add animation parameters and start animation
	animate:function animate(d,ease,delay){var akeys,skeys,key,element=this.target,fx=this;// dissect object if one is passed
	if((typeof d==='undefined'?'undefined':_typeof(d))=='object'){delay=d.delay;ease=d.ease;d=d.duration;}// ensure default duration and easing
	d=d=='='?d:d==null?1000:new SVG.Number(d).valueOf();ease=ease||'<>';// process values
	fx.at=function(pos){var i;// normalise pos
	pos=pos<0?0:pos>1?1:pos;// collect attribute keys
	if(akeys==null){akeys=[];for(key in fx.attrs){akeys.push(key);}// make sure morphable elements are scaled, translated and morphed all together
	if(element.morphArray&&(fx.destination.plot||akeys.indexOf('points')>-1)){// get destination
	var box,p=new element.morphArray(fx.destination.plot||fx.attrs.points||element.array());// add size
	if(fx.destination.size)p.size(fx.destination.size.width.to,fx.destination.size.height.to);// add movement
	box=p.bbox();if(fx.destination.x)p.move(fx.destination.x.to,box.y);else if(fx.destination.cx)p.move(fx.destination.cx.to-box.width/2,box.y);box=p.bbox();if(fx.destination.y)p.move(box.x,fx.destination.y.to);else if(fx.destination.cy)p.move(box.x,fx.destination.cy.to-box.height/2);// reset destination values
	fx.destination={plot:element.array().morph(p)};}}// collect style keys
	if(skeys==null){skeys=[];for(key in fx.styles){skeys.push(key);}}// apply easing
	pos=ease=='<>'?-Math.cos(pos*Math.PI)/2+0.5:ease=='>'?Math.sin(pos*Math.PI/2):ease=='<'?-Math.cos(pos*Math.PI/2)+1:ease=='-'?pos:typeof ease=='function'?ease(pos):pos;// run plot function
	if(fx.destination.plot){element.plot(fx.destination.plot.at(pos));}else{// run all x-position properties
	if(fx.destination.x)element.x(fx.destination.x.at(pos));else if(fx.destination.cx)element.cx(fx.destination.cx.at(pos));// run all y-position properties
	if(fx.destination.y)element.y(fx.destination.y.at(pos));else if(fx.destination.cy)element.cy(fx.destination.cy.at(pos));// run all size properties
	if(fx.destination.size)element.size(fx.destination.size.width.at(pos),fx.destination.size.height.at(pos));}// run all viewbox properties
	if(fx.destination.viewbox)element.viewbox(fx.destination.viewbox.x.at(pos),fx.destination.viewbox.y.at(pos),fx.destination.viewbox.width.at(pos),fx.destination.viewbox.height.at(pos));// run leading property
	if(fx.destination.leading)element.leading(fx.destination.leading.at(pos));// animate attributes
	for(i=akeys.length-1;i>=0;i--){element.attr(akeys[i],at(fx.attrs[akeys[i]],pos));}// animate styles
	for(i=skeys.length-1;i>=0;i--){element.style(skeys[i],at(fx.styles[skeys[i]],pos));}// callback for each keyframe
	if(fx.situation.during)fx.situation.during.call(element,pos,function(from,to){return at({from:from,to:to},pos);});};if(typeof d==='number'){// delay animation
	this.timeout=setTimeout(function(){var start=new Date().getTime();// initialize situation object
	fx.situation.start=start;fx.situation.play=true;fx.situation.finish=start+d;fx.situation.duration=d;fx.situation.ease=ease;// render function
	fx.render=function(){if(fx.situation.play===true){// calculate pos
	var time=new Date().getTime(),pos=time>fx.situation.finish?1:(time-fx.situation.start)/d;// reverse pos if animation is reversed
	if(fx.situation.reversing)pos=-pos+1;// process values
	fx.at(pos);// finish off animation
	if(time>fx.situation.finish){if(fx.destination.plot)element.plot(new SVG.PointArray(fx.destination.plot.destination).settle());if(fx.situation.loop===true||typeof fx.situation.loop=='number'&&fx.situation.loop>0){// register reverse
	if(fx.situation.reverse)fx.situation.reversing=!fx.situation.reversing;if(typeof fx.situation.loop=='number'){// reduce loop count
	if(!fx.situation.reverse||fx.situation.reversing)--fx.situation.loop;// remove last loop if reverse is disabled
	if(!fx.situation.reverse&&fx.situation.loop==1)--fx.situation.loop;}fx.animate(d,ease,delay);}else{fx.situation.after?fx.situation.after.apply(element,[fx]):fx.stop();}}else{fx.animationFrame=requestAnimationFrame(fx.render);}}else{fx.animationFrame=requestAnimationFrame(fx.render);}};// start animation
	fx.render();},new SVG.Number(delay).valueOf());}return this;}// Get bounding box of target element
	,bbox:function bbox(){return this.target.bbox();}// Add animatable attributes
	,attr:function attr(a,v){// apply attributes individually
	if((typeof a==='undefined'?'undefined':_typeof(a))=='object'){for(var key in a){this.attr(key,a[key]);}}else{// get the current state
	var from=this.target.attr(a);// detect format
	if(a=='transform'){// merge given transformation with an existing one
	if(this.attrs[a])v=this.attrs[a].destination.multiply(v);// prepare matrix for morphing
	this.attrs[a]=new SVG.Matrix(this.target).morph(v);// add parametric rotation values
	if(this.param){// get initial rotation
	v=this.target.transform('rotation');// add param
	this.attrs[a].param={from:this.target.param||{rotation:v,cx:this.param.cx,cy:this.param.cy},to:this.param};}}else{this.attrs[a]=SVG.Color.isColor(v)?// prepare color for morphing
	new SVG.Color(from).morph(v):SVG.regex.unit.test(v)?// prepare number for morphing
	new SVG.Number(from).morph(v):// prepare for plain morphing
	{from:from,to:v};}}return this;}// Add animatable styles
	,style:function style(s,v){if((typeof s==='undefined'?'undefined':_typeof(s))=='object')for(var key in s){this.style(key,s[key]);}else this.styles[s]={from:this.target.style(s),to:v};return this;}// Animatable x-axis
	,x:function x(_x2){this.destination.x=new SVG.Number(this.target.x()).morph(_x2);return this;}// Animatable y-axis
	,y:function y(_y2){this.destination.y=new SVG.Number(this.target.y()).morph(_y2);return this;}// Animatable center x-axis
	,cx:function cx(x){this.destination.cx=new SVG.Number(this.target.cx()).morph(x);return this;}// Animatable center y-axis
	,cy:function cy(y){this.destination.cy=new SVG.Number(this.target.cy()).morph(y);return this;}// Add animatable move
	,move:function move(x,y){return this.x(x).y(y);}// Add animatable center
	,center:function center(x,y){return this.cx(x).cy(y);}// Add animatable size
	,size:function size(width,height){if(this.target instanceof SVG.Text){// animate font size for Text elements
	this.attr('font-size',width);}else{// animate bbox based size for all other elements
	var box=this.target.bbox();this.destination.size={width:new SVG.Number(box.width).morph(width),height:new SVG.Number(box.height).morph(height)};}return this;}// Add animatable plot
	,plot:function plot(p){this.destination.plot=p;return this;}// Add leading method
	,leading:function leading(value){if(this.target.destination.leading)this.destination.leading=new SVG.Number(this.target.destination.leading).morph(value);return this;}// Add animatable viewbox
	,viewbox:function viewbox(x,y,width,height){if(this.target instanceof SVG.Container){var box=this.target.viewbox();this.destination.viewbox={x:new SVG.Number(box.x).morph(x),y:new SVG.Number(box.y).morph(y),width:new SVG.Number(box.width).morph(width),height:new SVG.Number(box.height).morph(height)};}return this;}// Add animateable gradient update
	,update:function update(o){if(this.target instanceof SVG.Stop){if(o.opacity!=null)this.attr('stop-opacity',o.opacity);if(o.color!=null)this.attr('stop-color',o.color);if(o.offset!=null)this.attr('offset',new SVG.Number(o.offset));}return this;}// Add callback for each keyframe
	,during:function during(_during){this.situation.during=_during;return this;}// Callback after animation
	,after:function after(_after){this.situation.after=_after;return this;}// Make loopable
	,loop:function loop(times,reverse){// store current loop and total loops
	this.situation.loop=this.situation.loops=times||true;// make reversable
	this.situation.reverse=!!reverse;return this;}// Stop running animation
	,stop:function stop(fulfill){// fulfill animation
	if(fulfill===true){this.animate(0);if(this.situation.after)this.situation.after.apply(this.target,[this]);}else{// stop current animation
	clearTimeout(this.timeout);cancelAnimationFrame(this.animationFrame);// reset storage for properties
	this.attrs={};this.styles={};this.situation={};this.destination={};}return this;}// Pause running animation
	,pause:function pause(){if(this.situation.play===true){this.situation.play=false;this.situation.pause=new Date().getTime();}return this;}// Play running animation
	,play:function play(){if(this.situation.play===false){var pause=new Date().getTime()-this.situation.pause;this.situation.finish+=pause;this.situation.start+=pause;this.situation.play=true;}return this;}}// Define parent class
	,parent:SVG.Element// Add method to parent elements
	,construct:{// Get fx module or create a new one, then animate with given duration and ease
	animate:function animate(d,ease,delay){return(this.fx||(this.fx=new SVG.FX(this))).stop().animate(d,ease,delay);}// Stop current animation; this is an alias to the fx instance
	,stop:function stop(fulfill){if(this.fx)this.fx.stop(fulfill);return this;}// Pause current animation
	,pause:function pause(){if(this.fx)this.fx.pause();return this;}// Play paused current animation
	,play:function play(){if(this.fx)this.fx.play();return this;}}});SVG.BBox=SVG.invent({// Initialize
	create:function create(element){// get values if element is given
	if(element){var box;// yes this is ugly, but Firefox can be a bitch when it comes to elements that are not yet rendered
	try{// find native bbox
	box=element.node.getBBox();}catch(e){if(element instanceof SVG.Shape){var clone=element.clone().addTo(SVG.parser.draw);box=clone.bbox();clone.remove();}else{box={x:element.node.clientLeft,y:element.node.clientTop,width:element.node.clientWidth,height:element.node.clientHeight};}}// plain x and y
	this.x=box.x;this.y=box.y;// plain width and height
	this.width=box.width;this.height=box.height;}// add center, right and bottom
	fullBox(this);}// Define Parent
	,parent:SVG.Element// Constructor
	,construct:{// Get bounding box
	bbox:function bbox(){return new SVG.BBox(this);}}});SVG.TBox=SVG.invent({// Initialize
	create:function create(element){// get values if element is given
	if(element){var t=element.ctm().extract(),box=element.bbox();// width and height including transformations
	this.width=box.width*t.scaleX;this.height=box.height*t.scaleY;// x and y including transformations
	this.x=box.x+t.x;this.y=box.y+t.y;}// add center, right and bottom
	fullBox(this);}// Define Parent
	,parent:SVG.Element// Constructor
	,construct:{// Get transformed bounding box
	tbox:function tbox(){return new SVG.TBox(this);}}});SVG.RBox=SVG.invent({// Initialize
	create:function create(element){if(element){var e=element.doc().parent(),box=element.node.getBoundingClientRect(),zoom=1;// get screen offset
	this.x=box.left;this.y=box.top;// subtract parent offset
	this.x-=e.offsetLeft;this.y-=e.offsetTop;while(e=e.offsetParent){this.x-=e.offsetLeft;this.y-=e.offsetTop;}// calculate cumulative zoom from svg documents
	e=element;while(e.parent&&(e=e.parent())){if(e.viewbox){zoom*=e.viewbox().zoom;this.x-=e.x()||0;this.y-=e.y()||0;}}// recalculate viewbox distortion
	this.width=box.width/=zoom;this.height=box.height/=zoom;}// add center, right and bottom
	fullBox(this);// offset by window scroll position, because getBoundingClientRect changes when window is scrolled
	this.x+=window.pageXOffset;this.y+=window.pageYOffset;}// define Parent
	,parent:SVG.Element// Constructor
	,construct:{// Get rect box
	rbox:function rbox(){return new SVG.RBox(this);}}})// Add universal merge method
	;[SVG.BBox,SVG.TBox,SVG.RBox].forEach(function(c){SVG.extend(c,{// Merge rect box with another, return a new instance
	merge:function merge(box){var b=new c();// merge boxes
	b.x=Math.min(this.x,box.x);b.y=Math.min(this.y,box.y);b.width=Math.max(this.x+this.width,box.x+box.width)-b.x;b.height=Math.max(this.y+this.height,box.y+box.height)-b.y;return fullBox(b);}});});SVG.Matrix=SVG.invent({// Initialize
	create:function create(source){var i,base=arrayToMatrix([1,0,0,1,0,0]);// ensure source as object
	source=source instanceof SVG.Element?source.matrixify():typeof source==='string'?stringToMatrix(source):arguments.length==6?arrayToMatrix([].slice.call(arguments)):(typeof source==='undefined'?'undefined':_typeof(source))==='object'?source:base;// merge source
	for(i=abcdef.length-1;i>=0;i--){this[abcdef[i]]=source&&typeof source[abcdef[i]]==='number'?source[abcdef[i]]:base[abcdef[i]];}}// Add methods
	,extend:{// Extract individual transformations
	extract:function extract(){// find delta transform points
	var px=deltaTransformPoint(this,0,1),py=deltaTransformPoint(this,1,0),skewX=180/Math.PI*Math.atan2(px.y,px.x)-90;return{// translation
	x:this.e,y:this.f// skew
	,skewX:-skewX,skewY:180/Math.PI*Math.atan2(py.y,py.x)// scale
	,scaleX:Math.sqrt(this.a*this.a+this.b*this.b),scaleY:Math.sqrt(this.c*this.c+this.d*this.d)// rotation
	,rotation:skewX,a:this.a,b:this.b,c:this.c,d:this.d,e:this.e,f:this.f};}// Clone matrix
	,clone:function clone(){return new SVG.Matrix(this);}// Morph one matrix into another
	,morph:function morph(matrix){// store new destination
	this.destination=new SVG.Matrix(matrix);return this;}// Get morphed matrix at a given position
	,at:function at(pos){// make sure a destination is defined
	if(!this.destination)return this;// calculate morphed matrix at a given position
	var matrix=new SVG.Matrix({a:this.a+(this.destination.a-this.a)*pos,b:this.b+(this.destination.b-this.b)*pos,c:this.c+(this.destination.c-this.c)*pos,d:this.d+(this.destination.d-this.d)*pos,e:this.e+(this.destination.e-this.e)*pos,f:this.f+(this.destination.f-this.f)*pos});// process parametric rotation if present
	if(this.param&&this.param.to){// calculate current parametric position
	var param={rotation:this.param.from.rotation+(this.param.to.rotation-this.param.from.rotation)*pos,cx:this.param.from.cx,cy:this.param.from.cy};// rotate matrix
	matrix=matrix.rotate((this.param.to.rotation-this.param.from.rotation*2)*pos,param.cx,param.cy);// store current parametric values
	matrix.param=param;}return matrix;}// Multiplies by given matrix
	,multiply:function multiply(matrix){return new SVG.Matrix(this.native().multiply(parseMatrix(matrix).native()));}// Inverses matrix
	,inverse:function inverse(){return new SVG.Matrix(this.native().inverse());}// Translate matrix
	,translate:function translate(x,y){return new SVG.Matrix(this.native().translate(x||0,y||0));}// Scale matrix
	,scale:function scale(x,y,cx,cy){// support universal scale
	if(arguments.length==1||arguments.length==3)y=x;if(arguments.length==3){cy=cx;cx=y;}return this.around(cx,cy,new SVG.Matrix(x,0,0,y,0,0));}// Rotate matrix
	,rotate:function rotate(r,cx,cy){// convert degrees to radians
	r=SVG.utils.radians(r);return this.around(cx,cy,new SVG.Matrix(Math.cos(r),Math.sin(r),-Math.sin(r),Math.cos(r),0,0));}// Flip matrix on x or y, at a given offset
	,flip:function flip(a,o){return a=='x'?this.scale(-1,1,o,0):this.scale(1,-1,0,o);}// Skew
	,skew:function skew(x,y,cx,cy){return this.around(cx,cy,this.native().skewX(x||0).skewY(y||0));}// SkewX
	,skewX:function skewX(x,cx,cy){return this.around(cx,cy,this.native().skewX(x||0));}// SkewY
	,skewY:function skewY(y,cx,cy){return this.around(cx,cy,this.native().skewY(y||0));}// Transform around a center point
	,around:function around(cx,cy,matrix){return this.multiply(new SVG.Matrix(1,0,0,1,cx||0,cy||0)).multiply(matrix).multiply(new SVG.Matrix(1,0,0,1,-cx||0,-cy||0));}// Convert to native SVGMatrix
	,native:function native(){// create new matrix
	var matrix=SVG.parser.draw.node.createSVGMatrix();// update with current values
	for(var i=abcdef.length-1;i>=0;i--){matrix[abcdef[i]]=this[abcdef[i]];}return matrix;}// Convert matrix to string
	,toString:function toString(){return'matrix('+this.a+','+this.b+','+this.c+','+this.d+','+this.e+','+this.f+')';}}// Define parent
	,parent:SVG.Element// Add parent method
	,construct:{// Get current matrix
	ctm:function ctm(){return new SVG.Matrix(this.node.getCTM());},// Get current screen matrix
	screenCTM:function screenCTM(){return new SVG.Matrix(this.node.getScreenCTM());}}});SVG.Point=SVG.invent({// Initialize
	create:function create(x,y){var i,source,base={x:0,y:0};// ensure source as object
	source=Array.isArray(x)?{x:x[0],y:x[1]}:(typeof x==='undefined'?'undefined':_typeof(x))==='object'?{x:x.x,y:x.y}:y!=null?{x:x,y:y}:base;// merge source
	this.x=source.x;this.y=source.y;}// Add methods
	,extend:{// Clone point
	clone:function clone(){return new SVG.Point(this);}// Morph one point into another
	,morph:function morph(point){// store new destination
	this.destination=new SVG.Point(point);return this;}// Get morphed point at a given position
	,at:function at(pos){// make sure a destination is defined
	if(!this.destination)return this;// calculate morphed matrix at a given position
	var point=new SVG.Point({x:this.x+(this.destination.x-this.x)*pos,y:this.y+(this.destination.y-this.y)*pos});return point;}// Convert to native SVGPoint
	,native:function native(){// create new point
	var point=SVG.parser.draw.node.createSVGPoint();// update with current values
	point.x=this.x;point.y=this.y;return point;}// transform point with matrix
	,transform:function transform(matrix){return new SVG.Point(this.native().matrixTransform(matrix.native()));}}});SVG.extend(SVG.Element,{// Get point
	point:function point(x,y){return new SVG.Point(x,y).transform(this.screenCTM().inverse());}});SVG.extend(SVG.Element,{// Set svg element attribute
	attr:function attr(a,v,n){// act as full getter
	if(a==null){// get an object of attributes
	a={};v=this.node.attributes;for(n=v.length-1;n>=0;n--){a[v[n].nodeName]=SVG.regex.isNumber.test(v[n].nodeValue)?parseFloat(v[n].nodeValue):v[n].nodeValue;}return a;}else if((typeof a==='undefined'?'undefined':_typeof(a))=='object'){// apply every attribute individually if an object is passed
	for(v in a){this.attr(v,a[v]);}}else if(v===null){// remove value
	this.node.removeAttribute(a);}else if(v==null){// act as a getter if the first and only argument is not an object
	v=this.node.getAttribute(a);return v==null?SVG.defaults.attrs[a]:SVG.regex.isNumber.test(v)?parseFloat(v):v;}else{// BUG FIX: some browsers will render a stroke if a color is given even though stroke width is 0
	if(a=='stroke-width')this.attr('stroke',parseFloat(v)>0?this._stroke:null);else if(a=='stroke')this._stroke=v;// convert image fill and stroke to patterns
	if(a=='fill'||a=='stroke'){if(SVG.regex.isImage.test(v))v=this.doc().defs().image(v,0,0);if(v instanceof SVG.Image)v=this.doc().defs().pattern(0,0,function(){this.add(v);});}// ensure correct numeric values (also accepts NaN and Infinity)
	if(typeof v==='number')v=new SVG.Number(v);// ensure full hex color
	else if(SVG.Color.isColor(v))v=new SVG.Color(v);// parse array values
	else if(Array.isArray(v))v=new SVG.Array(v);// store parametric transformation values locally
	else if(v instanceof SVG.Matrix&&v.param)this.param=v.param;// if the passed attribute is leading...
	if(a=='leading'){// ... call the leading method instead
	if(this.leading)this.leading(v);}else{// set given attribute on node
	typeof n==='string'?this.node.setAttributeNS(n,a,v.toString()):this.node.setAttribute(a,v.toString());}// rebuild if required
	if(this.rebuild&&(a=='font-size'||a=='x'))this.rebuild(a,v);}return this;}});SVG.extend(SVG.Element,SVG.FX,{// Add transformations
	transform:function transform(o,relative){// get target in case of the fx module, otherwise reference this
	var target=this.target||this,matrix;// act as a getter
	if((typeof o==='undefined'?'undefined':_typeof(o))!=='object'){// get current matrix
	matrix=new SVG.Matrix(target).extract();// add parametric rotation
	if(_typeof(this.param)==='object'){matrix.rotation=this.param.rotation;matrix.cx=this.param.cx;matrix.cy=this.param.cy;}return typeof o==='string'?matrix[o]:matrix;}// get current matrix
	matrix=this instanceof SVG.FX&&this.attrs.transform?this.attrs.transform:new SVG.Matrix(target);// ensure relative flag
	relative=!!relative||!!o.relative;// act on matrix
	if(o.a!=null){matrix=relative?// relative
	matrix.multiply(new SVG.Matrix(o)):// absolute
	new SVG.Matrix(o);// act on rotation
	}else if(o.rotation!=null){// ensure centre point
	ensureCentre(o,target);// relativize rotation value
	if(relative){o.rotation+=this.param&&this.param.rotation!=null?this.param.rotation:matrix.extract().rotation;}// store parametric values
	this.param=o;// apply transformation
	if(this instanceof SVG.Element){matrix=relative?// relative
	matrix.rotate(o.rotation,o.cx,o.cy):// absolute
	matrix.rotate(o.rotation-matrix.extract().rotation,o.cx,o.cy);}// act on scale
	}else if(o.scale!=null||o.scaleX!=null||o.scaleY!=null){// ensure centre point
	ensureCentre(o,target);// ensure scale values on both axes
	o.scaleX=o.scale!=null?o.scale:o.scaleX!=null?o.scaleX:1;o.scaleY=o.scale!=null?o.scale:o.scaleY!=null?o.scaleY:1;if(!relative){// absolute; multiply inversed values
	var e=matrix.extract();o.scaleX=o.scaleX*1/e.scaleX;o.scaleY=o.scaleY*1/e.scaleY;}matrix=matrix.scale(o.scaleX,o.scaleY,o.cx,o.cy);// act on skew
	}else if(o.skewX!=null||o.skewY!=null){// ensure centre point
	ensureCentre(o,target);// ensure skew values on both axes
	o.skewX=o.skewX!=null?o.skewX:0;o.skewY=o.skewY!=null?o.skewY:0;if(!relative){// absolute; reset skew values
	var e=matrix.extract();matrix=matrix.multiply(new SVG.Matrix().skew(e.skewX,e.skewY,o.cx,o.cy).inverse());}matrix=matrix.skew(o.skewX,o.skewY,o.cx,o.cy);// act on flip
	}else if(o.flip){matrix=matrix.flip(o.flip,o.offset==null?target.bbox()['c'+o.flip]:o.offset);// act on translate
	}else if(o.x!=null||o.y!=null){if(relative){// relative
	matrix=matrix.translate(o.x,o.y);}else{// absolute
	if(o.x!=null)matrix.e=o.x;if(o.y!=null)matrix.f=o.y;}}return this.attr(this instanceof SVG.Pattern?'patternTransform':this instanceof SVG.Gradient?'gradientTransform':'transform',matrix);}});SVG.extend(SVG.Element,{// Reset all transformations
	untransform:function untransform(){return this.attr('transform',null);},// merge the whole transformation chain into one matrix and returns it
	matrixify:function matrixify(){var matrix=(this.attr('transform')||'').// split transformations
	split(/\)\s*/).slice(0,-1).map(function(str){// generate key => value pairs
	var kv=str.trim().split('(');return[kv[0],kv[1].split(SVG.regex.matrixElements).map(function(str){return parseFloat(str);})];})// calculate every transformation into one matrix
	.reduce(function(matrix,transform){if(transform[0]=='matrix')return matrix.multiply(arrayToMatrix(transform[1]));return matrix[transform[0]].apply(matrix,transform[1]);},new SVG.Matrix());return matrix;},// add an element to another parent without changing the visual representation on the screen
	toParent:function toParent(parent){if(this==parent)return this;var ctm=this.screenCTM();var temp=parent.rect(1,1);var pCtm=temp.screenCTM().inverse();temp.remove();this.addTo(parent).untransform().transform(pCtm.multiply(ctm));return this;},// same as above with parent equals root-svg
	toDoc:function toDoc(){return this.toParent(this.doc());}});SVG.extend(SVG.Element,{// Dynamic style generator
	style:function style(s,v){if(arguments.length==0){// get full style
	return this.node.style.cssText||'';}else if(arguments.length<2){// apply every style individually if an object is passed
	if((typeof s==='undefined'?'undefined':_typeof(s))=='object'){for(v in s){this.style(v,s[v]);}}else if(SVG.regex.isCss.test(s)){// parse css string
	s=s.split(';');// apply every definition individually
	for(var i=0;i<s.length;i++){v=s[i].split(':');this.style(v[0].replace(/\s+/g,''),v[1]);}}else{// act as a getter if the first and only argument is not an object
	return this.node.style[camelCase(s)];}}else{this.node.style[camelCase(s)]=v===null||SVG.regex.isBlank.test(v)?'':v;}return this;}});SVG.Parent=SVG.invent({// Initialize node
	create:function create(element){this.constructor.call(this,element);}// Inherit from
	,inherit:SVG.Element// Add class methods
	,extend:{// Returns all child elements
	children:function children(){return SVG.utils.map(SVG.utils.filterSVGElements(this.node.childNodes),function(node){return SVG.adopt(node);});}// Add given element at a position
	,add:function add(element,i){if(!this.has(element)){// define insertion index if none given
	i=i==null?this.children().length:i;// add element references
	this.node.insertBefore(element.node,this.node.childNodes[i]||null);}return this;}// Basically does the same as `add()` but returns the added element instead
	,put:function put(element,i){this.add(element,i);return element;}// Checks if the given element is a child
	,has:function has(element){return this.index(element)>=0;}// Gets index of given element
	,index:function index(element){return this.children().indexOf(element);}// Get a element at the given index
	,get:function get(i){return this.children()[i];}// Get first child, skipping the defs node
	,first:function first(){return this.children()[0];}// Get the last child
	,last:function last(){return this.children()[this.children().length-1];}// Iterates over all children and invokes a given block
	,each:function each(block,deep){var i,il,children=this.children();for(i=0,il=children.length;i<il;i++){if(children[i]instanceof SVG.Element)block.apply(children[i],[i,children]);if(deep&&children[i]instanceof SVG.Container)children[i].each(block,deep);}return this;}// Remove a given child
	,removeElement:function removeElement(element){this.node.removeChild(element.node);return this;}// Remove all elements in this container
	,clear:function clear(){// remove children
	while(this.node.hasChildNodes()){this.node.removeChild(this.node.lastChild);}// remove defs reference
	delete this._defs;return this;},// Get defs
	defs:function defs(){return this.doc().defs();}}});SVG.extend(SVG.Parent,{ungroup:function ungroup(parent,depth){if(depth===0||this instanceof SVG.Defs)return this;parent=parent||(this instanceof SVG.Doc?this:this.parent(SVG.Parent));depth=depth||Infinity;this.each(function(){if(this instanceof SVG.Defs)return this;if(this instanceof SVG.Parent)return this.ungroup(parent,depth-1);return this.toParent(parent);});this.node.firstChild||this.remove();return this;},flatten:function flatten(parent,depth){return this.ungroup(parent,depth);}});SVG.Container=SVG.invent({// Initialize node
	create:function create(element){this.constructor.call(this,element);}// Inherit from
	,inherit:SVG.Parent// Add class methods
	,extend:{// Get the viewBox and calculate the zoom value
	viewbox:function viewbox(v){if(arguments.length==0)// act as a getter if there are no arguments
	return new SVG.ViewBox(this);// otherwise act as a setter
	v=arguments.length==1?[v.x,v.y,v.width,v.height]:[].slice.call(arguments);return this.attr('viewBox',v);}}})// Add events to elements
	;['click','dblclick','mousedown','mouseup','mouseover','mouseout','mousemove'// , 'mouseenter' -> not supported by IE
	// , 'mouseleave' -> not supported by IE
	,'touchstart','touchmove','touchleave','touchend','touchcancel'].forEach(function(event){// add event to SVG.Element
	SVG.Element.prototype[event]=function(f){var self=this;// bind event to element rather than element node
	this.node['on'+event]=typeof f=='function'?function(){return f.apply(self,arguments);}:null;return this;};});// Initialize listeners stack
	SVG.listeners=[];SVG.handlerMap=[];// Add event binder in the SVG namespace
	SVG.on=function(node,event,listener,binding){// create listener, get object-index
	var l=listener.bind(binding||node.instance||node),index=(SVG.handlerMap.indexOf(node)+1||SVG.handlerMap.push(node))-1,ev=event.split('.')[0],ns=event.split('.')[1]||'*';// ensure valid object
	SVG.listeners[index]=SVG.listeners[index]||{};SVG.listeners[index][ev]=SVG.listeners[index][ev]||{};SVG.listeners[index][ev][ns]=SVG.listeners[index][ev][ns]||{};// reference listener
	SVG.listeners[index][ev][ns][listener]=l;// add listener
	node.addEventListener(ev,l,false);};// Add event unbinder in the SVG namespace
	SVG.off=function(node,event,listener){var index=SVG.handlerMap.indexOf(node),ev=event&&event.split('.')[0],ns=event&&event.split('.')[1];if(index==-1)return;if(listener){// remove listener reference
	if(SVG.listeners[index][ev]&&SVG.listeners[index][ev][ns||'*']){// remove listener
	node.removeEventListener(ev,SVG.listeners[index][ev][ns||'*'][listener],false);delete SVG.listeners[index][ev][ns||'*'][listener];}}else if(ns&&ev){// remove all listeners for a namespaced event
	if(SVG.listeners[index][ev]&&SVG.listeners[index][ev][ns]){for(listener in SVG.listeners[index][ev][ns]){SVG.off(node,[ev,ns].join('.'),listener);}delete SVG.listeners[index][ev][ns];}}else if(ns){// remove all listeners for a specific namespace
	for(event in SVG.listeners[index]){for(namespace in SVG.listeners[index][event]){if(ns===namespace){SVG.off(node,[event,ns].join('.'));}}}}else if(ev){// remove all listeners for the event
	if(SVG.listeners[index][ev]){for(namespace in SVG.listeners[index][ev]){SVG.off(node,[ev,namespace].join('.'));}delete SVG.listeners[index][ev];}}else{// remove all listeners on a given node
	for(event in SVG.listeners[index]){SVG.off(node,event);}delete SVG.listeners[index];}};//
	SVG.extend(SVG.Element,{// Bind given event to listener
	on:function on(event,listener,binding){SVG.on(this.node,event,listener,binding);return this;}// Unbind event from listener
	,off:function off(event,listener){SVG.off(this.node,event,listener);return this;}// Fire given event
	,fire:function fire(event,data){// Dispatch event
	if(event instanceof Event){this.node.dispatchEvent(event);}else{this.node.dispatchEvent(new CustomEvent(event,{detail:data}));}return this;}});SVG.Defs=SVG.invent({// Initialize node
	create:'defs'// Inherit from
	,inherit:SVG.Container});SVG.G=SVG.invent({// Initialize node
	create:'g'// Inherit from
	,inherit:SVG.Container// Add class methods
	,extend:{// Move over x-axis
	x:function x(_x3){return _x3==null?this.transform('x'):this.transform({x:_x3-this.x()},true);}// Move over y-axis
	,y:function y(_y3){return _y3==null?this.transform('y'):this.transform({y:_y3-this.y()},true);}// Move by center over x-axis
	,cx:function cx(x){return x==null?this.tbox().cx:this.x(x-this.tbox().width/2);}// Move by center over y-axis
	,cy:function cy(y){return y==null?this.tbox().cy:this.y(y-this.tbox().height/2);},gbox:function gbox(){var bbox=this.bbox(),trans=this.transform();bbox.x+=trans.x;bbox.x2+=trans.x;bbox.cx+=trans.x;bbox.y+=trans.y;bbox.y2+=trans.y;bbox.cy+=trans.y;return bbox;}}// Add parent method
	,construct:{// Create a group element
	group:function group(){return this.put(new SVG.G());}}});// ### This module adds backward / forward functionality to elements.
	//
	SVG.extend(SVG.Element,{// Get all siblings, including myself
	siblings:function siblings(){return this.parent().children();}// Get the curent position siblings
	,position:function position(){return this.parent().index(this);}// Get the next element (will return null if there is none)
	,next:function next(){return this.siblings()[this.position()+1];}// Get the next element (will return null if there is none)
	,previous:function previous(){return this.siblings()[this.position()-1];}// Send given element one step forward
	,forward:function forward(){var i=this.position()+1,p=this.parent();// move node one step forward
	p.removeElement(this).add(this,i);// make sure defs node is always at the top
	if(p instanceof SVG.Doc)p.node.appendChild(p.defs().node);return this;}// Send given element one step backward
	,backward:function backward(){var i=this.position();if(i>0)this.parent().removeElement(this).add(this,i-1);return this;}// Send given element all the way to the front
	,front:function front(){var p=this.parent();// Move node forward
	p.node.appendChild(this.node);// Make sure defs node is always at the top
	if(p instanceof SVG.Doc)p.node.appendChild(p.defs().node);return this;}// Send given element all the way to the back
	,back:function back(){if(this.position()>0)this.parent().removeElement(this).add(this,0);return this;}// Inserts a given element before the targeted element
	,before:function before(element){element.remove();var i=this.position();this.parent().add(element,i);return this;}// Insters a given element after the targeted element
	,after:function after(element){element.remove();var i=this.position();this.parent().add(element,i+1);return this;}});SVG.Mask=SVG.invent({// Initialize node
	create:function create(){this.constructor.call(this,SVG.create('mask'));// keep references to masked elements
	this.targets=[];}// Inherit from
	,inherit:SVG.Container// Add class methods
	,extend:{// Unmask all masked elements and remove itself
	remove:function remove(){// unmask all targets
	for(var i=this.targets.length-1;i>=0;i--){if(this.targets[i])this.targets[i].unmask();}this.targets=[];// remove mask from parent
	this.parent().removeElement(this);return this;}}// Add parent method
	,construct:{// Create masking element
	mask:function mask(){return this.defs().put(new SVG.Mask());}}});SVG.extend(SVG.Element,{// Distribute mask to svg element
	maskWith:function maskWith(element){// use given mask or create a new one
	this.masker=element instanceof SVG.Mask?element:this.parent().mask().add(element);// store reverence on self in mask
	this.masker.targets.push(this);// apply mask
	return this.attr('mask','url("#'+this.masker.attr('id')+'")');}// Unmask element
	,unmask:function unmask(){delete this.masker;return this.attr('mask',null);}});SVG.ClipPath=SVG.invent({// Initialize node
	create:function create(){this.constructor.call(this,SVG.create('clipPath'));// keep references to clipped elements
	this.targets=[];}// Inherit from
	,inherit:SVG.Container// Add class methods
	,extend:{// Unclip all clipped elements and remove itself
	remove:function remove(){// unclip all targets
	for(var i=this.targets.length-1;i>=0;i--){if(this.targets[i])this.targets[i].unclip();}this.targets=[];// remove clipPath from parent
	this.parent().removeElement(this);return this;}}// Add parent method
	,construct:{// Create clipping element
	clip:function clip(){return this.defs().put(new SVG.ClipPath());}}});//
	SVG.extend(SVG.Element,{// Distribute clipPath to svg element
	clipWith:function clipWith(element){// use given clip or create a new one
	this.clipper=element instanceof SVG.ClipPath?element:this.parent().clip().add(element);// store reverence on self in mask
	this.clipper.targets.push(this);// apply mask
	return this.attr('clip-path','url("#'+this.clipper.attr('id')+'")');}// Unclip element
	,unclip:function unclip(){delete this.clipper;return this.attr('clip-path',null);}});SVG.Gradient=SVG.invent({// Initialize node
	create:function create(type){this.constructor.call(this,SVG.create(type+'Gradient'));// store type
	this.type=type;}// Inherit from
	,inherit:SVG.Container// Add class methods
	,extend:{// Add a color stop
	at:function at(offset,color,opacity){return this.put(new SVG.Stop()).update(offset,color,opacity);}// Update gradient
	,update:function update(block){// remove all stops
	this.clear();// invoke passed block
	if(typeof block=='function')block.call(this,this);return this;}// Return the fill id
	,fill:function fill(){return'url(#'+this.id()+')';}// Alias string convertion to fill
	,toString:function toString(){return this.fill();}// custom attr to handle transform
	,attr:function attr(a,b,c){if(a=='transform')a='gradientTransform';return SVG.Container.prototype.attr.call(this,a,b,c);}}// Add parent method
	,construct:{// Create gradient element in defs
	gradient:function gradient(type,block){return this.defs().gradient(type,block);}}});// Add animatable methods to both gradient and fx module
	SVG.extend(SVG.Gradient,SVG.FX,{// From position
	from:function from(x,y){return(this.target||this).type=='radial'?this.attr({fx:new SVG.Number(x),fy:new SVG.Number(y)}):this.attr({x1:new SVG.Number(x),y1:new SVG.Number(y)});}// To position
	,to:function to(x,y){return(this.target||this).type=='radial'?this.attr({cx:new SVG.Number(x),cy:new SVG.Number(y)}):this.attr({x2:new SVG.Number(x),y2:new SVG.Number(y)});}});// Base gradient generation
	SVG.extend(SVG.Defs,{// define gradient
	gradient:function gradient(type,block){return this.put(new SVG.Gradient(type)).update(block);}});SVG.Stop=SVG.invent({// Initialize node
	create:'stop'// Inherit from
	,inherit:SVG.Element// Add class methods
	,extend:{// add color stops
	update:function update(o){if(typeof o=='number'||o instanceof SVG.Number){o={offset:arguments[0],color:arguments[1],opacity:arguments[2]};}// set attributes
	if(o.opacity!=null)this.attr('stop-opacity',o.opacity);if(o.color!=null)this.attr('stop-color',o.color);if(o.offset!=null)this.attr('offset',new SVG.Number(o.offset));return this;}}});SVG.Pattern=SVG.invent({// Initialize node
	create:'pattern'// Inherit from
	,inherit:SVG.Container// Add class methods
	,extend:{// Return the fill id
	fill:function fill(){return'url(#'+this.id()+')';}// Update pattern by rebuilding
	,update:function update(block){// remove content
	this.clear();// invoke passed block
	if(typeof block=='function')block.call(this,this);return this;}// Alias string convertion to fill
	,toString:function toString(){return this.fill();}// custom attr to handle transform
	,attr:function attr(a,b,c){if(a=='transform')a='patternTransform';return SVG.Container.prototype.attr.call(this,a,b,c);}}// Add parent method
	,construct:{// Create pattern element in defs
	pattern:function pattern(width,height,block){return this.defs().pattern(width,height,block);}}});SVG.extend(SVG.Defs,{// Define gradient
	pattern:function pattern(width,height,block){return this.put(new SVG.Pattern()).update(block).attr({x:0,y:0,width:width,height:height,patternUnits:'userSpaceOnUse'});}});SVG.Doc=SVG.invent({// Initialize node
	create:function create(element){if(element){// ensure the presence of a dom element
	element=typeof element=='string'?document.getElementById(element):element;// If the target is an svg element, use that element as the main wrapper.
	// This allows svg.js to work with svg documents as well.
	if(element.nodeName=='svg'){this.constructor.call(this,element);}else{this.constructor.call(this,SVG.create('svg'));element.appendChild(this.node);}// set svg element attributes and ensure defs node
	this.namespace().size('100%','100%').defs();}}// Inherit from
	,inherit:SVG.Container// Add class methods
	,extend:{// Add namespaces
	namespace:function namespace(){return this.attr({xmlns:SVG.ns,version:'1.1'}).attr('xmlns:xlink',SVG.xlink,SVG.xmlns).attr('xmlns:svgjs',SVG.svgjs,SVG.xmlns);}// Creates and returns defs element
	,defs:function defs(){if(!this._defs){var defs;// Find or create a defs element in this instance
	if(defs=this.node.getElementsByTagName('defs')[0])this._defs=SVG.adopt(defs);else this._defs=new SVG.Defs();// Make sure the defs node is at the end of the stack
	this.node.appendChild(this._defs.node);}return this._defs;}// custom parent method
	,parent:function parent(){return this.node.parentNode.nodeName=='#document'?null:this.node.parentNode;}// Fix for possible sub-pixel offset. See:
	// https://bugzilla.mozilla.org/show_bug.cgi?id=608812
	,spof:function spof(_spof){var pos=this.node.getScreenCTM();if(pos)this.style('left',-pos.e%1+'px').style('top',-pos.f%1+'px');return this;}// Removes the doc from the DOM
	,remove:function remove(){if(this.parent()){this.parent().removeChild(this.node);}return this;}}});SVG.Shape=SVG.invent({// Initialize node
	create:function create(element){this.constructor.call(this,element);}// Inherit from
	,inherit:SVG.Element});SVG.Bare=SVG.invent({// Initialize
	create:function create(element,inherit){// construct element
	this.constructor.call(this,SVG.create(element));// inherit custom methods
	if(inherit)for(var method in inherit.prototype){if(typeof inherit.prototype[method]==='function')this[method]=inherit.prototype[method];}}// Inherit from
	,inherit:SVG.Element// Add methods
	,extend:{// Insert some plain text
	words:function words(text){// remove contents
	while(this.node.hasChildNodes()){this.node.removeChild(this.node.lastChild);}// create text node
	this.node.appendChild(document.createTextNode(text));return this;}}});SVG.extend(SVG.Parent,{// Create an element that is not described by SVG.js
	element:function element(_element,inherit){return this.put(new SVG.Bare(_element,inherit));}// Add symbol element
	,symbol:function symbol(){return this.defs().element('symbol',SVG.Container);}});SVG.Use=SVG.invent({// Initialize node
	create:'use'// Inherit from
	,inherit:SVG.Shape// Add class methods
	,extend:{// Use element as a reference
	element:function element(_element2,file){// Set lined element
	return this.attr('href',(file||'')+'#'+_element2,SVG.xlink);}}// Add parent method
	,construct:{// Create a use element
	use:function use(element,file){return this.put(new SVG.Use()).element(element,file);}}});SVG.Rect=SVG.invent({// Initialize node
	create:'rect'// Inherit from
	,inherit:SVG.Shape// Add parent method
	,construct:{// Create a rect element
	rect:function rect(width,height){return this.put(new SVG.Rect()).size(width,height);}}});SVG.Circle=SVG.invent({// Initialize node
	create:'circle'// Inherit from
	,inherit:SVG.Shape// Add parent method
	,construct:{// Create circle element, based on ellipse
	circle:function circle(size){return this.put(new SVG.Circle()).rx(new SVG.Number(size).divide(2)).move(0,0);}}});SVG.extend(SVG.Circle,SVG.FX,{// Radius x value
	rx:function rx(_rx){return this.attr('r',_rx);}// Alias radius x value
	,ry:function ry(_ry){return this.rx(_ry);}});SVG.Ellipse=SVG.invent({// Initialize node
	create:'ellipse'// Inherit from
	,inherit:SVG.Shape// Add parent method
	,construct:{// Create an ellipse
	ellipse:function ellipse(width,height){return this.put(new SVG.Ellipse()).size(width,height).move(0,0);}}});SVG.extend(SVG.Ellipse,SVG.Rect,SVG.FX,{// Radius x value
	rx:function rx(_rx2){return this.attr('rx',_rx2);}// Radius y value
	,ry:function ry(_ry2){return this.attr('ry',_ry2);}});// Add common method
	SVG.extend(SVG.Circle,SVG.Ellipse,{// Move over x-axis
	x:function x(_x4){return _x4==null?this.cx()-this.rx():this.cx(_x4+this.rx());}// Move over y-axis
	,y:function y(_y4){return _y4==null?this.cy()-this.ry():this.cy(_y4+this.ry());}// Move by center over x-axis
	,cx:function cx(x){return x==null?this.attr('cx'):this.attr('cx',x);}// Move by center over y-axis
	,cy:function cy(y){return y==null?this.attr('cy'):this.attr('cy',y);}// Set width of element
	,width:function width(_width2){return _width2==null?this.rx()*2:this.rx(new SVG.Number(_width2).divide(2));}// Set height of element
	,height:function height(_height2){return _height2==null?this.ry()*2:this.ry(new SVG.Number(_height2).divide(2));}// Custom size function
	,size:function size(width,height){var p=proportionalSize(this.bbox(),width,height);return this.rx(new SVG.Number(p.width).divide(2)).ry(new SVG.Number(p.height).divide(2));}});SVG.Line=SVG.invent({// Initialize node
	create:'line'// Inherit from
	,inherit:SVG.Shape// Add class methods
	,extend:{// Get array
	array:function array(){return new SVG.PointArray([[this.attr('x1'),this.attr('y1')],[this.attr('x2'),this.attr('y2')]]);}// Overwrite native plot() method
	,plot:function plot(x1,y1,x2,y2){if(arguments.length==4)x1={x1:x1,y1:y1,x2:x2,y2:y2};else x1=new SVG.PointArray(x1).toLine();return this.attr(x1);}// Move by left top corner
	,move:function move(x,y){return this.attr(this.array().move(x,y).toLine());}// Set element size to given width and height
	,size:function size(width,height){var p=proportionalSize(this.bbox(),width,height);return this.attr(this.array().size(p.width,p.height).toLine());}}// Add parent method
	,construct:{// Create a line element
	line:function line(x1,y1,x2,y2){return this.put(new SVG.Line()).plot(x1,y1,x2,y2);}}});SVG.Polyline=SVG.invent({// Initialize node
	create:'polyline'// Inherit from
	,inherit:SVG.Shape// Add parent method
	,construct:{// Create a wrapped polyline element
	polyline:function polyline(p){return this.put(new SVG.Polyline()).plot(p);}}});SVG.Polygon=SVG.invent({// Initialize node
	create:'polygon'// Inherit from
	,inherit:SVG.Shape// Add parent method
	,construct:{// Create a wrapped polygon element
	polygon:function polygon(p){return this.put(new SVG.Polygon()).plot(p);}}});// Add polygon-specific functions
	SVG.extend(SVG.Polyline,SVG.Polygon,{// Get array
	array:function array(){return this._array||(this._array=new SVG.PointArray(this.attr('points')));}// Plot new path
	,plot:function plot(p){return this.attr('points',this._array=new SVG.PointArray(p));}// Move by left top corner
	,move:function move(x,y){return this.attr('points',this.array().move(x,y));}// Set element size to given width and height
	,size:function size(width,height){var p=proportionalSize(this.bbox(),width,height);return this.attr('points',this.array().size(p.width,p.height));}});// unify all point to point elements
	SVG.extend(SVG.Line,SVG.Polyline,SVG.Polygon,{// Define morphable array
	morphArray:SVG.PointArray// Move by left top corner over x-axis
	,x:function x(_x5){return _x5==null?this.bbox().x:this.move(_x5,this.bbox().y);}// Move by left top corner over y-axis
	,y:function y(_y5){return _y5==null?this.bbox().y:this.move(this.bbox().x,_y5);}// Set width of element
	,width:function width(_width3){var b=this.bbox();return _width3==null?b.width:this.size(_width3,b.height);}// Set height of element
	,height:function height(_height3){var b=this.bbox();return _height3==null?b.height:this.size(b.width,_height3);}});SVG.Path=SVG.invent({// Initialize node
	create:'path'// Inherit from
	,inherit:SVG.Shape// Add class methods
	,extend:{// Define morphable array
	morphArray:SVG.PathArray// Get array
	,array:function array(){return this._array||(this._array=new SVG.PathArray(this.attr('d')));}// Plot new poly points
	,plot:function plot(p){return this.attr('d',this._array=new SVG.PathArray(p));}// Move by left top corner
	,move:function move(x,y){return this.attr('d',this.array().move(x,y));}// Move by left top corner over x-axis
	,x:function x(_x6){return _x6==null?this.bbox().x:this.move(_x6,this.bbox().y);}// Move by left top corner over y-axis
	,y:function y(_y6){return _y6==null?this.bbox().y:this.move(this.bbox().x,_y6);}// Set element size to given width and height
	,size:function size(width,height){var p=proportionalSize(this.bbox(),width,height);return this.attr('d',this.array().size(p.width,p.height));}// Set width of element
	,width:function width(_width4){return _width4==null?this.bbox().width:this.size(_width4,this.bbox().height);}// Set height of element
	,height:function height(_height4){return _height4==null?this.bbox().height:this.size(this.bbox().width,_height4);}}// Add parent method
	,construct:{// Create a wrapped path element
	path:function path(d){return this.put(new SVG.Path()).plot(d);}}});SVG.Image=SVG.invent({// Initialize node
	create:'image'// Inherit from
	,inherit:SVG.Shape// Add class methods
	,extend:{// (re)load image
	load:function load(url){if(!url)return this;var self=this,img=document.createElement('img');// preload image
	img.onload=function(){var p=self.parent(SVG.Pattern);if(p===null)return;// ensure image size
	if(self.width()==0&&self.height()==0)self.size(img.width,img.height);// ensure pattern size if not set
	if(p&&p.width()==0&&p.height()==0)p.size(self.width(),self.height());// callback
	if(typeof self._loaded==='function')self._loaded.call(self,{width:img.width,height:img.height,ratio:img.width/img.height,url:url});};return this.attr('href',img.src=this.src=url,SVG.xlink);}// Add loaded callback
	,loaded:function loaded(_loaded){this._loaded=_loaded;return this;}}// Add parent method
	,construct:{// create image element, load image and set its size
	image:function image(source,width,height){return this.put(new SVG.Image()).load(source).size(width||0,height||width||0);}}});SVG.Text=SVG.invent({// Initialize node
	create:function create(){this.constructor.call(this,SVG.create('text'));this.dom.leading=new SVG.Number(1.3);// store leading value for rebuilding
	this._rebuild=true;// enable automatic updating of dy values
	this._build=false;// disable build mode for adding multiple lines
	// set default font
	this.attr('font-family',SVG.defaults.attrs['font-family']);}// Inherit from
	,inherit:SVG.Shape// Add class methods
	,extend:{clone:function clone(){// clone element and assign new id
	var clone=assignNewId(this.node.cloneNode(true));// insert the clone after myself
	this.after(clone);return clone;}// Move over x-axis
	,x:function x(_x7){// act as getter
	if(_x7==null)return this.attr('x');// move lines as well if no textPath is present
	if(!this.textPath)this.lines().each(function(){if(this.dom.newLined)this.x(_x7);});return this.attr('x',_x7);}// Move over y-axis
	,y:function y(_y7){var oy=this.attr('y'),o=typeof oy==='number'?oy-this.bbox().y:0;// act as getter
	if(_y7==null)return typeof oy==='number'?oy-o:oy;return this.attr('y',typeof _y7==='number'?_y7+o:_y7);}// Move center over x-axis
	,cx:function cx(x){return x==null?this.bbox().cx:this.x(x-this.bbox().width/2);}// Move center over y-axis
	,cy:function cy(y){return y==null?this.bbox().cy:this.y(y-this.bbox().height/2);}// Set the text content
	,text:function text(text){// act as getter
	if(typeof text==='undefined'){var text='';var children=this.node.childNodes;for(var i=0,len=children.length;i<len;++i){// add newline if its not the first child and newLined is set to true
	if(i!=0&&children[i].nodeType!=3&&SVG.adopt(children[i]).dom.newLined==true){text+='\n';}// add content of this node
	text+=children[i].textContent;}return text;}// remove existing content
	this.clear().build(true);if(typeof text==='function'){// call block
	text.call(this,this);}else{// store text and make sure text is not blank
	text=text.split('\n');// build new lines
	for(var i=0,il=text.length;i<il;i++){this.tspan(text[i]).newLine();}}// disable build mode and rebuild lines
	return this.build(false).rebuild();}// Set font size
	,size:function size(_size){return this.attr('font-size',_size).rebuild();}// Set / get leading
	,leading:function leading(value){// act as getter
	if(value==null)return this.dom.leading;// act as setter
	this.dom.leading=new SVG.Number(value);return this.rebuild();}// Get all the first level lines
	,lines:function lines(){// filter tspans and map them to SVG.js instances
	var lines=SVG.utils.map(SVG.utils.filterSVGElements(this.node.childNodes),function(el){return SVG.adopt(el);});// return an instance of SVG.set
	return new SVG.Set(lines);}// Rebuild appearance type
	,rebuild:function rebuild(_rebuild){// store new rebuild flag if given
	if(typeof _rebuild=='boolean')this._rebuild=_rebuild;// define position of all lines
	if(this._rebuild){var self=this,blankLineOffset=0,dy=this.dom.leading*new SVG.Number(this.attr('font-size'));this.lines().each(function(){if(this.dom.newLined){if(!this.textPath)this.attr('x',self.attr('x'));if(this.text()=='\n'){blankLineOffset+=dy;}else{this.attr('dy',dy+blankLineOffset);blankLineOffset=0;}}});this.fire('rebuild');}return this;}// Enable / disable build mode
	,build:function build(_build){this._build=!!_build;return this;}// overwrite method from parent to set data properly
	,setData:function setData(o){this.dom=o;this.dom.leading=new SVG.Number(o.leading||1.3);return this;}}// Add parent method
	,construct:{// Create text element
	text:function text(_text){return this.put(new SVG.Text()).text(_text);}// Create plain text element
	,plain:function plain(text){return this.put(new SVG.Text()).plain(text);}}});SVG.Tspan=SVG.invent({// Initialize node
	create:'tspan'// Inherit from
	,inherit:SVG.Shape// Add class methods
	,extend:{// Set text content
	text:function text(_text2){if(_text2==null)return this.node.textContent+(this.dom.newLined?'\n':'');typeof _text2==='function'?_text2.call(this,this):this.plain(_text2);return this;}// Shortcut dx
	,dx:function dx(_dx){return this.attr('dx',_dx);}// Shortcut dy
	,dy:function dy(_dy){return this.attr('dy',_dy);}// Create new line
	,newLine:function newLine(){// fetch text parent
	var t=this.parent(SVG.Text);// mark new line
	this.dom.newLined=true;// apply new hy¡n
	return this.dy(t.dom.leading*t.attr('font-size')).attr('x',t.x());}}});SVG.extend(SVG.Text,SVG.Tspan,{// Create plain text node
	plain:function plain(text){// clear if build mode is disabled
	if(this._build===false)this.clear();// create text node
	this.node.appendChild(document.createTextNode(text));return this;}// Create a tspan
	,tspan:function tspan(text){var node=(this.textPath&&this.textPath()||this).node,tspan=new SVG.Tspan();// clear if build mode is disabled
	if(this._build===false)this.clear();// add new tspan
	node.appendChild(tspan.node);return tspan.text(text);}// Clear all lines
	,clear:function clear(){var node=(this.textPath&&this.textPath()||this).node;// remove existing child nodes
	while(node.hasChildNodes()){node.removeChild(node.lastChild);}return this;}// Get length of text element
	,length:function length(){return this.node.getComputedTextLength();}});SVG.TextPath=SVG.invent({// Initialize node
	create:'textPath'// Inherit from
	,inherit:SVG.Element// Define parent class
	,parent:SVG.Text// Add parent method
	,construct:{// Create path for text to run on
	path:function path(d){// create textPath element
	var path=new SVG.TextPath(),track=this.doc().defs().path(d);// move lines to textpath
	while(this.node.hasChildNodes()){path.node.appendChild(this.node.firstChild);}// add textPath element as child node
	this.node.appendChild(path.node);// link textPath to path and add content
	path.attr('href','#'+track,SVG.xlink);return this;}// Plot path if any
	,plot:function plot(d){var track=this.track();if(track)track.plot(d);return this;}// Get the path track element
	,track:function track(){var path=this.textPath();if(path)return path.reference('href');}// Get the textPath child
	,textPath:function textPath(){if(this.node.firstChild&&this.node.firstChild.nodeName=='textPath')return SVG.adopt(this.node.firstChild);}}});SVG.Nested=SVG.invent({// Initialize node
	create:function create(){this.constructor.call(this,SVG.create('svg'));this.style('overflow','visible');}// Inherit from
	,inherit:SVG.Container// Add parent method
	,construct:{// Create nested svg document
	nested:function nested(){return this.put(new SVG.Nested());}}});SVG.A=SVG.invent({// Initialize node
	create:'a'// Inherit from
	,inherit:SVG.Container// Add class methods
	,extend:{// Link url
	to:function to(url){return this.attr('href',url,SVG.xlink);}// Link show attribute
	,show:function show(target){return this.attr('show',target,SVG.xlink);}// Link target attribute
	,target:function target(_target){return this.attr('target',_target);}}// Add parent method
	,construct:{// Create a hyperlink element
	link:function link(url){return this.put(new SVG.A()).to(url);}}});SVG.extend(SVG.Element,{// Create a hyperlink element
	linkTo:function linkTo(url){var link=new SVG.A();if(typeof url=='function')url.call(link,link);else link.to(url);return this.parent().put(link).put(this);}});SVG.Marker=SVG.invent({// Initialize node
	create:'marker'// Inherit from
	,inherit:SVG.Container// Add class methods
	,extend:{// Set width of element
	width:function width(_width5){return this.attr('markerWidth',_width5);}// Set height of element
	,height:function height(_height5){return this.attr('markerHeight',_height5);}// Set marker refX and refY
	,ref:function ref(x,y){return this.attr('refX',x).attr('refY',y);}// Update marker
	,update:function update(block){// remove all content
	this.clear();// invoke passed block
	if(typeof block=='function')block.call(this,this);return this;}// Return the fill id
	,toString:function toString(){return'url(#'+this.id()+')';}}// Add parent method
	,construct:{marker:function marker(width,height,block){// Create marker element in defs
	return this.defs().marker(width,height,block);}}});SVG.extend(SVG.Defs,{// Create marker
	marker:function marker(width,height,block){// Set default viewbox to match the width and height, set ref to cx and cy and set orient to auto
	return this.put(new SVG.Marker()).size(width,height).ref(width/2,height/2).viewbox(0,0,width,height).attr('orient','auto').update(block);}});SVG.extend(SVG.Line,SVG.Polyline,SVG.Polygon,SVG.Path,{// Create and attach markers
	marker:function marker(_marker,width,height,block){var attr=['marker'];// Build attribute name
	if(_marker!='all')attr.push(_marker);attr=attr.join('-');// Set marker attribute
	_marker=arguments[1]instanceof SVG.Marker?arguments[1]:this.doc().marker(width,height,block);return this.attr(attr,_marker);}});// Define list of available attributes for stroke and fill
	var sugar={stroke:['color','width','opacity','linecap','linejoin','miterlimit','dasharray','dashoffset'],fill:['color','opacity','rule'],prefix:function prefix(t,a){return a=='color'?t:t+'-'+a;}}// Add sugar for fill and stroke
	;['fill','stroke'].forEach(function(m){var i,extension={};extension[m]=function(o){if(typeof o=='string'||SVG.Color.isRgb(o)||o&&typeof o.fill==='function')this.attr(m,o);else// set all attributes from sugar.fill and sugar.stroke list
	for(i=sugar[m].length-1;i>=0;i--){if(o[sugar[m][i]]!=null)this.attr(sugar.prefix(m,sugar[m][i]),o[sugar[m][i]]);}return this;};SVG.extend(SVG.Element,SVG.FX,extension);});SVG.extend(SVG.Element,SVG.FX,{// Map rotation to transform
	rotate:function rotate(d,cx,cy){return this.transform({rotation:d,cx:cx,cy:cy});}// Map skew to transform
	,skew:function skew(x,y,cx,cy){return this.transform({skewX:x,skewY:y,cx:cx,cy:cy});}// Map scale to transform
	,scale:function scale(x,y,cx,cy){return arguments.length==1||arguments.length==3?this.transform({scale:x,cx:y,cy:cx}):this.transform({scaleX:x,scaleY:y,cx:cx,cy:cy});}// Map translate to transform
	,translate:function translate(x,y){return this.transform({x:x,y:y});}// Map flip to transform
	,flip:function flip(a,o){return this.transform({flip:a,offset:o});}// Map matrix to transform
	,matrix:function matrix(m){return this.attr('transform',new SVG.Matrix(m));}// Opacity
	,opacity:function opacity(value){return this.attr('opacity',value);}// Relative move over x axis
	,dx:function dx(x){return this.x((this.target||this).x()+x);}// Relative move over y axis
	,dy:function dy(y){return this.y((this.target||this).y()+y);}// Relative move over x and y axes
	,dmove:function dmove(x,y){return this.dx(x).dy(y);}});SVG.extend(SVG.Rect,SVG.Ellipse,SVG.Circle,SVG.Gradient,SVG.FX,{// Add x and y radius
	radius:function radius(x,y){var type=(this.target||this).type;return type=='radial'||type=='circle'?this.attr({'r':new SVG.Number(x)}):this.rx(x).ry(y==null?x:y);}});SVG.extend(SVG.Path,{// Get path length
	length:function length(){return this.node.getTotalLength();}// Get point at length
	,pointAt:function pointAt(length){return this.node.getPointAtLength(length);}});SVG.extend(SVG.Parent,SVG.Text,SVG.FX,{// Set font
	font:function font(o){for(var k in o){k=='leading'?this.leading(o[k]):k=='anchor'?this.attr('text-anchor',o[k]):k=='size'||k=='family'||k=='weight'||k=='stretch'||k=='variant'||k=='style'?this.attr('font-'+k,o[k]):this.attr(k,o[k]);}return this;}});SVG.Set=SVG.invent({// Initialize
	create:function create(members){// Set initial state
	Array.isArray(members)?this.members=members:this.clear();}// Add class methods
	,extend:{// Add element to set
	add:function add(){var i,il,elements=[].slice.call(arguments);for(i=0,il=elements.length;i<il;i++){this.members.push(elements[i]);}return this;}// Remove element from set
	,remove:function remove(element){var i=this.index(element);// remove given child
	if(i>-1)this.members.splice(i,1);return this;}// Iterate over all members
	,each:function each(block){for(var i=0,il=this.members.length;i<il;i++){block.apply(this.members[i],[i,this.members]);}return this;}// Restore to defaults
	,clear:function clear(){// initialize store
	this.members=[];return this;}// Get the length of a set
	,length:function length(){return this.members.length;}// Checks if a given element is present in set
	,has:function has(element){return this.index(element)>=0;}// retuns index of given element in set
	,index:function index(element){return this.members.indexOf(element);}// Get member at given index
	,get:function get(i){return this.members[i];}// Get first member
	,first:function first(){return this.get(0);}// Get last member
	,last:function last(){return this.get(this.members.length-1);}// Default value
	,valueOf:function valueOf(){return this.members;}// Get the bounding box of all members included or empty box if set has no items
	,bbox:function bbox(){var box=new SVG.BBox();// return an empty box of there are no members
	if(this.members.length==0)return box;// get the first rbox and update the target bbox
	var rbox=this.members[0].rbox();box.x=rbox.x;box.y=rbox.y;box.width=rbox.width;box.height=rbox.height;this.each(function(){// user rbox for correct position and visual representation
	box=box.merge(this.rbox());});return box;}}// Add parent method
	,construct:{// Create a new set
	set:function set(members){return new SVG.Set(members);}}});SVG.FX.Set=SVG.invent({// Initialize node
	create:function create(set){// store reference to set
	this.set=set;}});// Alias methods
	SVG.Set.inherit=function(){var m,methods=[];// gather shape methods
	for(var m in SVG.Shape.prototype){if(typeof SVG.Shape.prototype[m]=='function'&&typeof SVG.Set.prototype[m]!='function')methods.push(m);}// apply shape aliasses
	methods.forEach(function(method){SVG.Set.prototype[method]=function(){for(var i=0,il=this.members.length;i<il;i++){if(this.members[i]&&typeof this.members[i][method]=='function')this.members[i][method].apply(this.members[i],arguments);}return method=='animate'?this.fx||(this.fx=new SVG.FX.Set(this)):this;};});// clear methods for the next round
	methods=[];// gather fx methods
	for(var m in SVG.FX.prototype){if(typeof SVG.FX.prototype[m]=='function'&&typeof SVG.FX.Set.prototype[m]!='function')methods.push(m);}// apply fx aliasses
	methods.forEach(function(method){SVG.FX.Set.prototype[method]=function(){for(var i=0,il=this.set.members.length;i<il;i++){this.set.members[i].fx[method].apply(this.set.members[i].fx,arguments);}return this;};});};SVG.extend(SVG.Element,{// Store data values on svg nodes
	data:function data(a,v,r){if((typeof a==='undefined'?'undefined':_typeof(a))=='object'){for(v in a){this.data(v,a[v]);}}else if(arguments.length<2){try{return JSON.parse(this.attr('data-'+a));}catch(e){return this.attr('data-'+a);}}else{this.attr('data-'+a,v===null?null:r===true||typeof v==='string'||typeof v==='number'?v:JSON.stringify(v));}return this;}});SVG.extend(SVG.Element,{// Remember arbitrary data
	remember:function remember(k,v){// remember every item in an object individually
	if(_typeof(arguments[0])=='object')for(var v in k){this.remember(v,k[v]);}// retrieve memory
	else if(arguments.length==1)return this.memory()[k];// store memory
	else this.memory()[k]=v;return this;}// Erase a given memory
	,forget:function forget(){if(arguments.length==0)this._memory={};else for(var i=arguments.length-1;i>=0;i--){delete this.memory()[arguments[i]];}return this;}// Initialize or return local memory object
	,memory:function memory(){return this._memory||(this._memory={});}});// Method for getting an element by id
	SVG.get=function(id){var node=document.getElementById(idFromReference(id)||id);return SVG.adopt(node);};// Select elements by query string
	SVG.select=function(query,parent){return new SVG.Set(SVG.utils.map((parent||document).querySelectorAll(query),function(node){return SVG.adopt(node);}));};SVG.extend(SVG.Parent,{// Scoped select method
	select:function select(query){return SVG.select(query,this.node);}});// tests if a given selector matches an element
	function _matches(el,selector){return(el.matches||el.matchesSelector||el.msMatchesSelector||el.mozMatchesSelector||el.webkitMatchesSelector||el.oMatchesSelector).call(el,selector);}// Convert dash-separated-string to camelCase
	function camelCase(s){return s.toLowerCase().replace(/-(.)/g,function(m,g){return g.toUpperCase();});}// Capitalize first letter of a string
	function capitalize(s){return s.charAt(0).toUpperCase()+s.slice(1);}// Ensure to six-based hex
	function fullHex(hex){return hex.length==4?['#',hex.substring(1,2),hex.substring(1,2),hex.substring(2,3),hex.substring(2,3),hex.substring(3,4),hex.substring(3,4)].join(''):hex;}// Component to hex value
	function compToHex(comp){var hex=comp.toString(16);return hex.length==1?'0'+hex:hex;}// Calculate proportional width and height values when necessary
	function proportionalSize(box,width,height){if(height==null)height=box.height/box.width*width;else if(width==null)width=box.width/box.height*height;return{width:width,height:height};}// Delta transform point
	function deltaTransformPoint(matrix,x,y){return{x:x*matrix.a+y*matrix.c+0,y:x*matrix.b+y*matrix.d+0};}// Map matrix array to object
	function arrayToMatrix(a){return{a:a[0],b:a[1],c:a[2],d:a[3],e:a[4],f:a[5]};}// Parse matrix if required
	function parseMatrix(matrix){if(!(matrix instanceof SVG.Matrix))matrix=new SVG.Matrix(matrix);return matrix;}// Add centre point to transform object
	function ensureCentre(o,target){o.cx=o.cx==null?target.bbox().cx:o.cx;o.cy=o.cy==null?target.bbox().cy:o.cy;}// Convert string to matrix
	function stringToMatrix(source){// remove matrix wrapper and split to individual numbers
	source=source.replace(SVG.regex.whitespace,'').replace(SVG.regex.matrix,'').split(SVG.regex.matrixElements);// convert string values to floats and convert to a matrix-formatted object
	return arrayToMatrix(SVG.utils.map(source,function(n){return parseFloat(n);}));}// Calculate position according to from and to
	function at(o,pos){// number recalculation (don't bother converting to SVG.Number for performance reasons)
	return typeof o.from=='number'?o.from+(o.to-o.from)*pos:// instance recalculation
	o instanceof SVG.Color||o instanceof SVG.Number||o instanceof SVG.Matrix?o.at(pos):// for all other values wait until pos has reached 1 to return the final value
	pos<1?o.from:o.to;}// PathArray Helpers
	function arrayToString(a){for(var i=0,il=a.length,s='';i<il;i++){s+=a[i][0];if(a[i][1]!=null){s+=a[i][1];if(a[i][2]!=null){s+=' ';s+=a[i][2];if(a[i][3]!=null){s+=' ';s+=a[i][3];s+=' ';s+=a[i][4];if(a[i][5]!=null){s+=' ';s+=a[i][5];s+=' ';s+=a[i][6];if(a[i][7]!=null){s+=' ';s+=a[i][7];}}}}}}return s+' ';}// Deep new id assignment
	function assignNewId(node){// do the same for SVG child nodes as well
	for(var i=node.childNodes.length-1;i>=0;i--){if(node.childNodes[i]instanceof SVGElement)assignNewId(node.childNodes[i]);}return SVG.adopt(node).id(SVG.eid(node.nodeName));}// Add more bounding box properties
	function fullBox(b){if(b.x==null){b.x=0;b.y=0;b.width=0;b.height=0;}b.w=b.width;b.h=b.height;b.x2=b.x+b.width;b.y2=b.y+b.height;b.cx=b.x+b.width/2;b.cy=b.y+b.height/2;return b;}// Get id from reference string
	function idFromReference(url){var m=url.toString().match(SVG.regex.reference);if(m)return m[1];}// Create matrix array for looping
	var abcdef='abcdef'.split('');// Add CustomEvent to IE9 and IE10
	if(typeof CustomEvent!=='function'){// Code from: https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent
	var CustomEvent=function CustomEvent(event,options){options=options||{bubbles:false,cancelable:false,detail:undefined};var e=document.createEvent('CustomEvent');e.initCustomEvent(event,options.bubbles,options.cancelable,options.detail);return e;};CustomEvent.prototype=window.Event.prototype;window.CustomEvent=CustomEvent;}// requestAnimationFrame / cancelAnimationFrame Polyfill with fallback based on Paul Irish
	(function(w){var lastTime=0;var vendors=['moz','webkit'];for(var x=0;x<vendors.length&&!window.requestAnimationFrame;++x){w.requestAnimationFrame=w[vendors[x]+'RequestAnimationFrame'];w.cancelAnimationFrame=w[vendors[x]+'CancelAnimationFrame']||w[vendors[x]+'CancelRequestAnimationFrame'];}w.requestAnimationFrame=w.requestAnimationFrame||function(callback){var currTime=new Date().getTime();var timeToCall=Math.max(0,16-(currTime-lastTime));var id=w.setTimeout(function(){callback(currTime+timeToCall);},timeToCall);lastTime=currTime+timeToCall;return id;};w.cancelAnimationFrame=w.cancelAnimationFrame||w.clearTimeout;})(window);return SVG;});

/***/ },
/* 4 */
/***/ function(module, exports) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	    value: true
	});
	function findAncestor(element, className) {
	    if (element === null) {
	        return null;
	    }
	    while ((element = element.parentElement) && element && !hasClass(element, className)) {}
	    return element;
	}

	function hasClass(element, cls) {
	    return (' ' + element.className + ' ').indexOf(' ' + cls + ' ') > -1;
	}

	exports.findAncestor = findAncestor;
	exports.hasClass = hasClass;

/***/ },
/* 5 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	    value: true
	});
	exports.initTooltip = undefined;

	var _utils = __webpack_require__(4);

	var TOOLTIP_TIMEOUT = 500;

	var timeoutId = null;
	var currentTooltip = null;
	var cursorX = 0;
	var cursorY = 0;

	function initTooltip(container) {
	    disableCssTooltips(container);
	    trackMousePosition();

	    var nodes = container.querySelectorAll('.qp-node');

	    var _loop = function _loop(i) {
	        var node = nodes[i];
	        node.addEventListener("mouseover", function () {
	            onMouseover(node);
	        });
	        node.addEventListener("mouseout", function (event) {
	            onMouseout(node, event);
	        });
	    };

	    for (var i = 0; i < nodes.length; i++) {
	        _loop(i);
	    }
	}

	function disableCssTooltips(container) {
	    var root = container.querySelector(".qp-root");
	    root.className += " qp-noCssTooltip";
	}

	function trackMousePosition() {
	    document.onmousemove = function (e) {
	        cursorX = e.pageX;
	        cursorY = e.pageY;
	    };
	}

	function onMouseover(node) {
	    if (timeoutId != null) {
	        return;
	    }
	    timeoutId = window.setTimeout(function () {
	        showTooltip(node);
	    }, TOOLTIP_TIMEOUT);
	}

	function onMouseout(node, event) {
	    // http://stackoverflow.com/questions/4697758/prevent-onmouseout-when-hovering-child-element-of-the-parent-absolute-div-withou
	    var e = event.toElement || event.relatedTarget;
	    if (e == node || (0, _utils.findAncestor)(e, 'qp-node') == node || currentTooltip != null && (e == currentTooltip || (0, _utils.findAncestor)(e, 'qp-tt') == currentTooltip)) {
	        return;
	    }
	    window.clearTimeout(timeoutId);
	    timeoutId = null;
	    hideTooltip();
	}

	function showTooltip(node) {
	    hideTooltip();

	    var positionY = cursorY;
	    var tooltip = node.querySelector(".qp-tt");

	    // Nudge the tooptip up if its going to appear below the bottom of the page
	    var documentHeight = getDocumentHeight();
	    var gapAtBottom = documentHeight - (positionY + tooltip.offsetHeight);
	    if (gapAtBottom < 10) {
	        positionY = documentHeight - (tooltip.offsetHeight + 10);
	    }

	    // Stop the tooltip appearing above the top of the page
	    if (positionY < 10) {
	        positionY = 10;
	    }

	    currentTooltip = tooltip.cloneNode(true);
	    document.body.appendChild(currentTooltip);
	    currentTooltip.style.left = cursorX + 'px';
	    currentTooltip.style.top = positionY + 'px';
	    currentTooltip.addEventListener("mouseout", function (event) {
	        onMouseout(node, event);
	    });
	}

	function getDocumentHeight() {
	    // http://stackoverflow.com/a/1147768/113141
	    var body = document.body,
	        html = document.documentElement;
	    return Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight);
	}

	function hideTooltip() {
	    if (currentTooltip != null) {
	        document.body.removeChild(currentTooltip);
	        currentTooltip = null;
	    }
	}

	exports.initTooltip = initTooltip;

/***/ },
/* 6 */
/***/ function(module, exports) {

	module.exports = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\r\n  xmlns:msxsl=\"urn:schemas-microsoft-com:xslt\"\r\n  xmlns:exslt=\"http://exslt.org/common\"\r\n  xmlns:s=\"http://schemas.microsoft.com/sqlserver/2004/07/showplan\"\r\n  exclude-result-prefixes=\"msxsl s xsl\">\r\n  <xsl:output method=\"html\" indent=\"no\" omit-xml-declaration=\"yes\" />\r\n\r\n  <!-- Disable built-in recursive processing templates -->\r\n  <xsl:template match=\"*|/|text()|@*\" mode=\"NodeLabel\" />\r\n  <xsl:template match=\"*|/|text()|@*\" mode=\"NodeLabel2\" />\r\n  <xsl:template match=\"*|/|text()|@*\" mode=\"ToolTipDescription\" />\r\n  <xsl:template match=\"*|/|text()|@*\" mode=\"ToolTipDetails\" />\r\n\r\n  <!-- Default template -->\r\n  <xsl:template match=\"/\">\r\n    <xsl:apply-templates select=\"s:ShowPlanXML\" />\r\n  </xsl:template>\r\n\r\n  <!-- Outermost div that contains all statement plans. -->\r\n  <xsl:template match=\"s:ShowPlanXML\">\r\n    <div class=\"qp-root\">\r\n      <xsl:apply-templates select=\"s:BatchSequence/s:Batch/s:Statements/*\" mode=\"Statement\" />  \r\n    </div>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:BatchSequence/s:Batch/s:Statements/*\" mode=\"Statement\">\r\n    <div class=\"qp-statement-header\">\r\n      <div><xsl:value-of select=\"@StatementText\" /></div>\r\n    </div>\r\n    <xsl:apply-templates select=\".\" mode=\"QpTr\" />\r\n  </xsl:template>\r\n  \r\n  <!-- Each node has a parent qp-tr element which contains / positions the node and its children -->\r\n  <xsl:template match=\"s:RelOp|s:StmtSimple|s:StmtUseDb|s:StmtCond|s:StmtCursor|s:Operation\" mode=\"QpTr\">\r\n    <div class=\"qp-tr\">\r\n      <xsl:if test=\"@StatementId\">\r\n        <xsl:attribute name=\"data-statement-id\"><xsl:value-of select=\"@StatementId\" /></xsl:attribute>\r\n      </xsl:if>\r\n      <div>\r\n        <div class=\"qp-node\">\r\n          <xsl:apply-templates select=\".\" mode=\"NodeIcon\" />\r\n          <div><xsl:apply-templates select=\".\" mode=\"NodeLabel\" /></div>\r\n          <xsl:apply-templates select=\".\" mode=\"NodeLabel2\" />\r\n          <xsl:apply-templates select=\".\" mode=\"NodeCostLabel\" />\r\n          <xsl:call-template name=\"ToolTip\" />\r\n        </div>\r\n      </div>\r\n      <div><xsl:apply-templates select=\"*/*\" mode=\"QpTr\" /></div>\r\n    </div>\r\n  </xsl:template>\r\n\r\n  <!-- Writes the tool tip -->\r\n  <xsl:template name=\"ToolTip\">\r\n    <div class=\"qp-tt\">\r\n      <div class=\"qp-tt-header\"><xsl:apply-templates select=\".\" mode=\"NodeLabel\" /></div>\r\n      <div><xsl:apply-templates select=\".\" mode=\"ToolTipDescription\" /></div>\r\n      <xsl:call-template name=\"ToolTipGrid\" />\r\n      <xsl:apply-templates select=\"* | @* | */* | */@*\" mode=\"ToolTipDetails\" />\r\n    </div>\r\n  </xsl:template>\r\n\r\n  <!-- Writes the grid of node properties to the tool tip -->\r\n  <xsl:template name=\"ToolTipGrid\">\r\n    <table>\r\n    \r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"s:QueryPlan/@CachedPlanSize\" />\r\n        <xsl:with-param name=\"Label\">Cached plan size</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"concat(s:QueryPlan/@CachedPlanSize, ' B')\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"@PhysicalOp\" />\r\n        <xsl:with-param name=\"Label\">Physical Operation</xsl:with-param>\r\n        <xsl:with-param name=\"Value\">          \r\n          <xsl:apply-templates select=\".\" mode=\"PhysicalOperation\" />\r\n        </xsl:with-param>\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"@LogicalOp\" />\r\n        <xsl:with-param name=\"Label\">Logical Operation</xsl:with-param>\r\n        <xsl:with-param name=\"Value\">          \r\n          <xsl:apply-templates select=\".\" mode=\"LogicalOperation\" />\r\n        </xsl:with-param>\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"s:RunTimeInformation\" />\r\n        <xsl:with-param name=\"Label\">Actual Execution Mode</xsl:with-param>\r\n        <xsl:with-param name=\"Value\">\r\n          <xsl:choose>\r\n            <xsl:when test=\"s:RunTimeInformation/s:RunTimeCountersPerThread/@ActualExecutionMode\">\r\n              <xsl:value-of select=\"s:RunTimeInformation/s:RunTimeCountersPerThread/@ActualExecutionMode\" />\r\n            </xsl:when>\r\n            <xsl:otherwise>Row</xsl:otherwise>\r\n          </xsl:choose>\r\n        </xsl:with-param>\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Estimated Execution Mode</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"@EstimatedExecutionMode\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Storage</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"s:IndexScan/@Storage|s:TableScan/@Storage\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Number of Rows Read</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"sum(s:RunTimeInformation/s:RunTimeCountersPerThread/@ActualRowsRead)\" />\r\n      </xsl:call-template>\r\n      \r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Actual Number of Rows</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"sum(s:RunTimeInformation/s:RunTimeCountersPerThread/@ActualRows)\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"s:RunTimeInformation\" />\r\n        <xsl:with-param name=\"Label\">Actual Number of Batches</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"sum(s:RunTimeInformation/s:RunTimeCountersPerThread/@Batches)\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"@EstimateIO\" />\r\n        <xsl:with-param name=\"Label\">Estimated I/O Cost</xsl:with-param>\r\n        <xsl:with-param name=\"Value\">\r\n          <xsl:call-template name=\"round\">\r\n            <xsl:with-param name=\"value\" select=\"@EstimateIO\" />\r\n          </xsl:call-template>\r\n        </xsl:with-param>\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"@EstimateCPU\" />\r\n        <xsl:with-param name=\"Label\">Estimated CPU Cost</xsl:with-param>\r\n        <xsl:with-param name=\"Value\">\r\n          <xsl:call-template name=\"round\">\r\n            <xsl:with-param name=\"value\" select=\"@EstimateCPU\" />\r\n          </xsl:call-template>\r\n        </xsl:with-param>\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Number of Executions</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"sum(s:RunTimeInformation/s:RunTimeCountersPerThread/@ActualExecutions)\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Estimated Number of Executions</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"@EstimateRebinds + 1\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Degree of Parallelism</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"s:QueryPlan/@DegreeOfParallelism\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Memory Grant</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"s:QueryPlan/@MemoryGrant\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"@EstimateIO | @EstimateCPU\" />\r\n        <xsl:with-param name=\"Label\">Estimated Operator Cost</xsl:with-param>\r\n        <xsl:with-param name=\"Value\">\r\n          <xsl:variable name=\"EstimatedOperatorCost\">\r\n            <xsl:call-template name=\"EstimatedOperatorCost\" />\r\n          </xsl:variable>\r\n          <xsl:variable name=\"TotalCost\">\r\n            <xsl:value-of select=\"ancestor::s:QueryPlan/s:RelOp/@EstimatedTotalSubtreeCost\" />\r\n          </xsl:variable>\r\n          <xsl:call-template name=\"round\">\r\n            <xsl:with-param name=\"value\" select=\"$EstimatedOperatorCost\" />\r\n          </xsl:call-template> (<xsl:value-of select=\"format-number(number($EstimatedOperatorCost) div number($TotalCost), '0%')\" />)</xsl:with-param>\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"@StatementSubTreeCost | @EstimatedTotalSubtreeCost\" />\r\n        <xsl:with-param name=\"Label\">Estimated Subtree Cost</xsl:with-param>\r\n        <xsl:with-param name=\"Value\">\r\n          <xsl:call-template name=\"round\">\r\n            <xsl:with-param name=\"value\" select=\"@StatementSubTreeCost | @EstimatedTotalSubtreeCost\" />\r\n          </xsl:call-template>\r\n        </xsl:with-param>\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Estimated Number of Rows to be Read</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"@EstimatedRowsRead\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Estimated Number of Rows</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"@StatementEstRows | @EstimateRows\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"@AvgRowSize\" />\r\n        <xsl:with-param name=\"Label\">Estimated Row Size</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"concat(@AvgRowSize, ' B')\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"s:RunTimeInformation\" />\r\n        <xsl:with-param name=\"Label\">Actual Rebinds</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"sum(s:RunTimeInformation/s:RunTimeCountersPerThread/@ActualRebinds)\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"s:RunTimeInformation\" />\r\n        <xsl:with-param name=\"Label\">Actual Rewinds</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"sum(s:RunTimeInformation/s:RunTimeCountersPerThread/@ActualRewinds)\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Condition\" select=\"s:IndexScan/@Ordered\" />\r\n        <xsl:with-param name=\"Label\">Ordered</xsl:with-param>\r\n        <xsl:with-param name=\"Value\">\r\n          <xsl:choose>\r\n            <xsl:when test=\"s:IndexScan/@Ordered = 'true'\">True</xsl:when>\r\n            <xsl:when test=\"s:IndexScan/@Ordered = 1\">True</xsl:when>\r\n            <xsl:otherwise>False</xsl:otherwise>\r\n          </xsl:choose>\r\n        </xsl:with-param>\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Partitioning Type</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"s:Parallelism/@PartitioningType\" />\r\n      </xsl:call-template>\r\n\r\n      <xsl:call-template name=\"ToolTipRow\">\r\n        <xsl:with-param name=\"Label\">Node ID</xsl:with-param>\r\n        <xsl:with-param name=\"Value\" select=\"@NodeId\" />\r\n      </xsl:call-template>\r\n\r\n    </table>\r\n  </xsl:template>\r\n\r\n  <!-- Gets the Physical Operation -->\r\n  <xsl:template match=\"s:RelOp\" mode=\"PhysicalOperation\">\r\n    <xsl:value-of select=\"@PhysicalOp\" />\r\n  </xsl:template>\r\n  <xsl:template match=\"s:RelOp[s:IndexScan/@Lookup]\" mode=\"PhysicalOperation\">Key Lookup</xsl:template>\r\n  \r\n  <!-- Gets the Logical Operation -->\r\n  <xsl:template match=\"s:RelOp\" mode=\"LogicalOperation\">\r\n    <xsl:value-of select=\"@LogicalOp\" />\r\n  </xsl:template>\r\n  <xsl:template match=\"s:RelOp[s:IndexScan/@Lookup]\" mode=\"LogicalOperation\">Key Lookup</xsl:template>\r\n  \r\n  <!-- Calculates the estimated operator cost. -->\r\n  <xsl:template name=\"EstimatedOperatorCost\">\r\n    <xsl:variable name=\"EstimatedTotalSubtreeCost\">\r\n      <xsl:call-template name=\"convertSciToNumString\">\r\n        <xsl:with-param name=\"inputVal\" select=\"@EstimatedTotalSubtreeCost\" />\r\n      </xsl:call-template>\r\n    </xsl:variable>\r\n    <xsl:variable name=\"ChildEstimatedSubtreeCost\">\r\n      <xsl:for-each select=\"*/s:RelOp\">\r\n        <value>\r\n          <xsl:call-template name=\"convertSciToNumString\">\r\n            <xsl:with-param name=\"inputVal\" select=\"@EstimatedTotalSubtreeCost\" />\r\n          </xsl:call-template>\r\n        </value>\r\n      </xsl:for-each>\r\n    </xsl:variable>\r\n    <xsl:variable name=\"TotalChildEstimatedSubtreeCost\">\r\n      <xsl:choose>\r\n        <xsl:when test=\"function-available('exslt:node-set')\">\r\n          <xsl:value-of select='sum(exslt:node-set($ChildEstimatedSubtreeCost)/value)' />\r\n        </xsl:when>\r\n        <xsl:when test=\"function-available('msxsl:node-set')\">\r\n          <xsl:value-of select='sum(msxsl:node-set($ChildEstimatedSubtreeCost)/value)' />\r\n        </xsl:when>\r\n      </xsl:choose>\r\n    </xsl:variable>\r\n    <xsl:choose>\r\n      <xsl:when test=\"number($EstimatedTotalSubtreeCost) - number($TotalChildEstimatedSubtreeCost) &lt; 0\">0</xsl:when>\r\n      <xsl:otherwise>\r\n        <xsl:value-of select=\"number($EstimatedTotalSubtreeCost) - number($TotalChildEstimatedSubtreeCost)\" />\r\n      </xsl:otherwise>\r\n    </xsl:choose>\r\n  </xsl:template>\r\n\r\n  <!-- Renders a row in the tool tip details table. -->\r\n  <xsl:template name=\"ToolTipRow\">\r\n    <xsl:param name=\"Label\" />\r\n    <xsl:param name=\"Value\" />\r\n    <xsl:param name=\"Condition\" select=\"$Value\" />\r\n    <xsl:if test=\"$Condition\">\r\n      <tr>\r\n        <th><xsl:value-of select=\"$Label\" /></th>\r\n        <td><xsl:value-of select=\"$Value\" /></td>\r\n      </tr>\r\n    </xsl:if>\r\n  </xsl:template>\r\n\r\n  <!-- Prints the name of an object. -->\r\n  <xsl:template match=\"s:Object | s:ColumnReference\" mode=\"ObjectName\">\r\n    <xsl:param name=\"ExcludeDatabaseName\" select=\"false()\" />\r\n    <xsl:choose>\r\n      <xsl:when test=\"$ExcludeDatabaseName\">\r\n        <xsl:for-each select=\"@Table | @Index | @Column | @Alias\">\r\n          <xsl:value-of select=\".\" />\r\n          <xsl:if test=\"position() != last()\">.</xsl:if>\r\n        </xsl:for-each>\r\n      </xsl:when>\r\n      <xsl:otherwise>\r\n        <xsl:for-each select=\"@Database | @Schema | @Table | @Index | @Column | @Alias\">\r\n          <xsl:value-of select=\".\" />\r\n          <xsl:if test=\"position() != last()\">.</xsl:if>\r\n        </xsl:for-each>\r\n      </xsl:otherwise>\r\n    </xsl:choose>\r\n  </xsl:template>\r\n  \r\n  <xsl:template match=\"s:Object | s:ColumnReference\" mode=\"ObjectNameNoAlias\">\r\n    <xsl:for-each select=\"@Database | @Schema | @Table | @Index | @Column\">\r\n      <xsl:value-of select=\".\" />\r\n      <xsl:if test=\"position() != last()\">.</xsl:if>\r\n    </xsl:for-each>\r\n  </xsl:template>\r\n\r\n  <!-- Displays the node cost label. -->    \r\n  <xsl:template match=\"s:RelOp\" mode=\"NodeCostLabel\">\r\n    <xsl:variable name=\"EstimatedOperatorCost\"><xsl:call-template name=\"EstimatedOperatorCost\" /></xsl:variable>\r\n    <xsl:variable name=\"TotalCost\"><xsl:value-of select=\"ancestor::s:QueryPlan/s:RelOp/@EstimatedTotalSubtreeCost\" /></xsl:variable>\r\n    <div>Cost: <xsl:value-of select=\"format-number(number($EstimatedOperatorCost) div number($TotalCost), '0%')\" /></div>\r\n  </xsl:template>\r\n\r\n  <!-- Dont show the node cost for statements. -->\r\n  <xsl:template match=\"s:StmtSimple|s:StmtUseDb\" mode=\"NodeCostLabel\" />\r\n\r\n  <xsl:template match=\"s:StmtCursor|s:Operation|s:StmtCond\" mode=\"NodeCostLabel\">\r\n    <div>Cost: 0%</div>\r\n  </xsl:template>\r\n\r\n  <!-- \r\n  ================================\r\n  Tool tip detail sections\r\n  ================================\r\n  The following section contains templates used for writing the detail sections at the bottom of the tool tip,\r\n  for example listing outputs, or information about the object to which an operator applies.\r\n  -->\r\n\r\n  <xsl:template match=\"*/s:Object\" mode=\"ToolTipDetails\">\r\n    <!-- TODO: Make sure this works all the time -->\r\n    <div class=\"qp-bold\">Object</div>\r\n    <div><xsl:apply-templates select=\".\" mode=\"ObjectName\" /></div>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:SetPredicate[s:ScalarOperator/@ScalarString]\" mode=\"ToolTipDetails\">\r\n    <div class=\"qp-bold\">Predicate</div>\r\n    <div><xsl:value-of select=\"s:ScalarOperator/@ScalarString\" /></div>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:Predicate[s:ScalarOperator/@ScalarString]\" mode=\"ToolTipDetails\">\r\n    <div class=\"qp-bold\">Predicate</div>\r\n    <div><xsl:value-of select=\"s:ScalarOperator/@ScalarString\" /></div>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:TopExpression[s:ScalarOperator/@ScalarString]\" mode=\"ToolTipDetails\">\r\n    <div class=\"qp-bold\">Top Expression</div>\r\n    <div><xsl:value-of select=\"s:ScalarOperator/@ScalarString\" /></div>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:OutputList[count(s:ColumnReference) > 0]\" mode=\"ToolTipDetails\">\r\n    <div class=\"qp-bold\">Output List</div>\r\n    <xsl:for-each select=\"s:ColumnReference\">\r\n      <div><xsl:apply-templates select=\".\" mode=\"ObjectName\" /></div>\r\n    </xsl:for-each>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:NestedLoops/s:OuterReferences[count(s:ColumnReference) > 0]\" mode=\"ToolTipDetails\">\r\n    <div class=\"qp-bold\">Outer References</div>\r\n    <xsl:for-each select=\"s:ColumnReference\">\r\n      <div><xsl:apply-templates select=\".\" mode=\"ObjectName\" /></div>\r\n    </xsl:for-each>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"@StatementText\" mode=\"ToolTipDetails\">\r\n    <div class=\"qp-bold\">Statement</div>\r\n    <div><xsl:value-of select=\".\" /></div>\r\n  </xsl:template>\r\n  \r\n  <xsl:template match=\"s:StmtSimple/s:StoredProc\" mode=\"ToolTipDetails\">\r\n    <div class=\"qp-bold\">Procedure Name</div>\r\n    <div><xsl:value-of select=\"@ProcName\" /></div>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:Sort/s:OrderBy[count(s:OrderByColumn/s:ColumnReference) > 0]\" mode=\"ToolTipDetails\">\r\n    <div class=\"qp-bold\">Order By</div>\r\n    <xsl:for-each select=\"s:OrderByColumn\">\r\n      <div>\r\n        <xsl:apply-templates select=\"s:ColumnReference\" mode=\"ObjectName\" />\r\n        <xsl:choose>\r\n          <xsl:when test=\"@Ascending = 'true'\"> Ascending</xsl:when>\r\n          <xsl:when test=\"@Ascending = 1\"> Ascending</xsl:when>\r\n          <xsl:otherwise> Descending</xsl:otherwise>\r\n        </xsl:choose>\r\n      </div>\r\n    </xsl:for-each>\r\n  </xsl:template>\r\n\r\n  <!-- \r\n  Seek Predicates Tooltip\r\n  -->\r\n\r\n  <xsl:template match=\"s:SeekPredicates\" mode=\"ToolTipDetails\">\r\n    <div class=\"qp-bold\">Seek Predicates</div>\r\n    <div>\r\n      <xsl:for-each select=\"s:SeekPredicateNew/s:SeekKeys\">\r\n        <xsl:call-template name=\"SeekKeyDetail\">\r\n          <xsl:with-param name=\"position\" select=\"position()\" />\r\n        </xsl:call-template>\r\n        <xsl:if test=\"position() != last()\">, </xsl:if>\r\n      </xsl:for-each>\r\n    </div>\r\n  </xsl:template>\r\n\r\n  <xsl:template name=\"SeekKeyDetail\">\r\n    <xsl:param name=\"position\" />Seek Keys[<xsl:value-of select=\"$position\" />]: <xsl:for-each select=\"s:Prefix|s:StartRange|s:EndRange\">\r\n      <xsl:choose>\r\n        <xsl:when test=\"self::s:Prefix\">Prefix: </xsl:when>\r\n        <xsl:when test=\"self::s:StartRange\">Start: </xsl:when>\r\n        <xsl:when test=\"self::s:EndRange\">End: </xsl:when>\r\n      </xsl:choose>\r\n      <xsl:for-each select=\"s:RangeColumns/s:ColumnReference\">\r\n        <xsl:apply-templates select=\".\" mode=\"ObjectNameNoAlias\" />\r\n        <xsl:if test=\"position() != last()\">, </xsl:if>\r\n      </xsl:for-each>\r\n      <xsl:choose>\r\n        <xsl:when test=\"@ScanType = 'EQ'\"> = </xsl:when>\r\n        <xsl:when test=\"@ScanType = 'LT'\"> &lt; </xsl:when>\r\n        <xsl:when test=\"@ScanType = 'GT'\"> > </xsl:when>\r\n        <xsl:when test=\"@ScanType = 'LE'\"> &lt;= </xsl:when>\r\n        <xsl:when test=\"@ScanType = 'GE'\"> >= </xsl:when>\r\n      </xsl:choose>\r\n      <xsl:for-each select=\"s:RangeExpressions/s:ScalarOperator\">Scalar Operator(<xsl:value-of select=\"@ScalarString\" />)<xsl:if test=\"position() != last()\">, </xsl:if>\r\n      </xsl:for-each>\r\n      <xsl:if test=\"position() != last()\">, </xsl:if>\r\n    </xsl:for-each>\r\n  </xsl:template>\r\n\r\n  <!-- \r\n  ================================\r\n  Node icons\r\n  ================================\r\n  The following templates determine what icon should be shown for a given node\r\n  -->\r\n\r\n  <!-- Use the logical operation to determine the icon for the \"Parallelism\" operators. -->\r\n  <xsl:template match=\"s:RelOp[@PhysicalOp = 'Parallelism']\" mode=\"NodeIcon\" priority=\"1\">\r\n    <xsl:element name=\"div\">\r\n      <xsl:attribute name=\"class\">qp-icon-<xsl:value-of select=\"translate(@LogicalOp, ' ', '')\" /></xsl:attribute>\r\n    </xsl:element>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"*[s:CursorPlan/@CursorActualType]\" mode=\"NodeIcon\" priority=\"1\">\r\n    <xsl:element name=\"div\">\r\n      <xsl:attribute name=\"class\">qp-icon-<xsl:value-of select=\"s:CursorPlan/@CursorActualType\" /></xsl:attribute>\r\n    </xsl:element>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"*[@OperationType]\" mode=\"NodeIcon\" priority=\"1\">\r\n    <xsl:element name=\"div\">\r\n      <xsl:attribute name=\"class\">qp-icon-<xsl:value-of select=\"@OperationType\" /></xsl:attribute>\r\n    </xsl:element>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:RelOp[s:IndexScan/@Lookup]\" mode=\"NodeIcon\" priority=\"1\">\r\n    <div class=\"qp-icon-KeyLookup\"></div>\r\n  </xsl:template>\r\n \r\n  <xsl:template match=\"s:RelOp[s:TableValuedFunction]\" mode=\"NodeIcon\" priority=\"1\">\r\n    <div class=\"qp-icon-TableValuedFunction\"></div>\r\n  </xsl:template>\r\n\r\n  <!-- Use the physical operation to determine icon if it is present. -->\r\n  <xsl:template match=\"*[@PhysicalOp]\" mode=\"NodeIcon\">\r\n    <xsl:element name=\"div\">\r\n      <xsl:attribute name=\"class\">qp-icon-<xsl:value-of select=\"translate(@PhysicalOp, ' ', '')\" /></xsl:attribute>\r\n    </xsl:element>\r\n  </xsl:template>\r\n  \r\n  <!-- Matches all statements. -->\r\n  <xsl:template match=\"s:StmtSimple\" mode=\"NodeIcon\">\r\n    <div class=\"qp-icon-Statement\"></div>\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:StmtCursor\" mode=\"NodeIcon\">\r\n    <div class=\"qp-icon-StmtCursor\"></div>\r\n  </xsl:template>\r\n\r\n  <!-- Fallback template - show the Bitmap icon. -->\r\n  <xsl:template match=\"*\" mode=\"NodeIcon\">\r\n    <div class=\"qp-icon-Catchall\"></div>\r\n  </xsl:template>\r\n\r\n  <!-- \r\n  ================================\r\n  Node labels\r\n  ================================\r\n  The following section contains templates used to determine the first (main) label for a node.\r\n  -->\r\n\r\n  <xsl:template match=\"s:RelOp\" mode=\"NodeLabel\">\r\n    <xsl:value-of select=\"@PhysicalOp\" />\r\n  </xsl:template>\r\n\r\n  <xsl:template match=\"s:RelOp[s:IndexScan/@Lookup]\" mode=\"NodeLabel\">Key Lookup (Clustered)</xsl:template>\r\n\r\n  <xsl:template match=\"*[@StatementType]\" mode=\"NodeLabel\">\r\n    <xsl:value-of select=\"@StatementType\" />\r\n  </xsl:template>\r\n  \r\n  <xsl:template match=\"*[s:StoredProc]\" mode=\"NodeLabel\">Stored Procedure</xsl:template>\r\n\r\n  <xsl:template match=\"*[s:CursorPlan/@CursorActualType = 'Dynamic']\" mode=\"NodeLabel\">Dynamic</xsl:template>\r\n  <xsl:template match=\"*[s:CursorPlan/@CursorActualType = 'FastForward']\" mode=\"NodeLabel\">Fast Forward</xsl:template>\r\n  <xsl:template match=\"*[s:CursorPlan/@CursorActualType = 'Keyset']\" mode=\"NodeLabel\">Keyset</xsl:template>\r\n  <xsl:template match=\"*[s:CursorPlan/@CursorActualType = 'SnapShot']\" mode=\"NodeLabel\">Snapshot</xsl:template>  \r\n\r\n  <xsl:template match=\"*[@OperationType = 'FetchQuery']\" mode=\"NodeLabel\">Fetch Query</xsl:template>\r\n  <xsl:template match=\"*[@OperationType = 'PopulateQuery']\" mode=\"NodeLabel\">Population Query</xsl:template>\r\n  <xsl:template match=\"*[@OperationType = 'RefreshQuery']\" mode=\"NodeLabel\">Refresh Query</xsl:template>\r\n  \r\n  <!--\r\n  ================================\r\n  Node alternate labels\r\n  ================================\r\n  The following section contains templates used to determine the second label to be displayed for a node.\r\n  -->\r\n\r\n  <!-- Display the object for any node that has one -->\r\n  <xsl:template match=\"*[*/s:Object]\" mode=\"NodeLabel2\">\r\n    <xsl:variable name=\"ObjectName\">\r\n      <xsl:apply-templates select=\"*/s:Object\" mode=\"ObjectName\">\r\n        <xsl:with-param name=\"ExcludeDatabaseName\" select=\"true()\" />\r\n      </xsl:apply-templates>\r\n    </xsl:variable>\r\n    <div>\r\n      <xsl:value-of select=\"substring($ObjectName, 0, 36)\" />\r\n      <xsl:if test=\"string-length($ObjectName) >= 36\">…</xsl:if>\r\n    </div>\r\n  </xsl:template>\r\n\r\n  <!-- Display the logical operation for any node where it is not the same as the physical operation. -->\r\n  <xsl:template match=\"s:RelOp[@LogicalOp != @PhysicalOp]\" mode=\"NodeLabel2\">\r\n    <div>(<xsl:value-of select=\"@LogicalOp\" />)</div>\r\n  </xsl:template>\r\n\r\n  <!-- Disable the default template -->\r\n  <xsl:template match=\"*\" mode=\"NodeLabel2\" />\r\n\r\n  <!-- \r\n  ================================\r\n  Tool tip descriptions\r\n  ================================\r\n  The following section contains templates used for writing the description shown in the tool tip.\r\n  -->\r\n\r\n  <xsl:template match=\"*[@PhysicalOp = 'Table Insert']\" mode=\"ToolTipDescription\">Insert input rows into the table specified in Argument field.</xsl:template>\r\n  <xsl:template match=\"*[@PhysicalOp = 'Compute Scalar']\" mode=\"ToolTipDescription\">Compute new values from existing values in a row.</xsl:template>\r\n  <xsl:template match=\"*[@PhysicalOp = 'Sort']\" mode=\"ToolTipDescription\">Sort the input.</xsl:template>\r\n  <xsl:template match=\"*[@PhysicalOp = 'Clustered Index Scan']\" mode=\"ToolTipDescription\">Scanning a clustered index, entirely or only a range.</xsl:template>\r\n  <xsl:template match=\"*[@PhysicalOp = 'Stream Aggregate']\" mode=\"ToolTipDescription\">Compute summary values for groups of rows in a suitably sorted stream.</xsl:template>\r\n  <xsl:template match=\"*[@PhysicalOp = 'Hash Match']\" mode=\"ToolTipDescription\">Use each row from the top input to build a hash table, and each row from the bottom input to probe into the hash table, outputting all matching rows.</xsl:template>\r\n  <xsl:template match=\"*[@PhysicalOp = 'Bitmap']\" mode=\"ToolTipDescription\">Bitmap.</xsl:template>\r\n  <xsl:template match=\"*[@PhysicalOp = 'Clustered Index Seek']\" mode=\"ToolTipDescription\">Scanning a particular range of rows from a clustered index.</xsl:template>\r\n  <xsl:template match=\"*[@PhysicalOp = 'Index Seek']\" mode=\"ToolTipDescription\">Scan a particular range of rows from a nonclustered index.</xsl:template>\r\n  <xsl:template match=\"*[s:IndexScan/@Lookup]\" mode=\"ToolTipDescription\">Uses a supplied clustering key to lookup on a table that has a clustered index.</xsl:template>\r\n\r\n  <xsl:template match=\"*[@PhysicalOp = 'Parallelism' and @LogicalOp='Repartition Streams']\" mode=\"ToolTipDescription\">Repartition Streams.</xsl:template>\r\n  <xsl:template match=\"*[@PhysicalOp = 'Parallelism']\" mode=\"ToolTipDescription\">An operation involving parallelism.</xsl:template>\r\n  \r\n  <xsl:template match=\"*[s:TableScan]\" mode=\"ToolTipDescription\">Scan rows from a table.</xsl:template>\r\n  <xsl:template match=\"*[s:NestedLoops]\" mode=\"ToolTipDescription\">For each row in the top (outer) input, scan the bottom (inner) input, and output matching rows.</xsl:template>\r\n  <xsl:template match=\"*[s:Top]\" mode=\"ToolTipDescription\">Select the first few rows based on a sort order.</xsl:template>\r\n  \r\n  <xsl:template match=\"*[@OperationType='FetchQuery']\" mode=\"ToolTipDescription\">The query used to retrieve rows when a fetch is issued against a cursor.</xsl:template>\r\n  <xsl:template match=\"*[@OperationType='PopulateQuery']\" mode=\"ToolTipDescription\">The query used to populate a cursor's work table when the cursor is opened.</xsl:template>\r\n  <xsl:template match=\"*[s:CursorPlan/@CursorActualType='FastForward']\" mode=\"ToolTipDescription\">Fast Forward.</xsl:template>\r\n  <xsl:template match=\"*[s:CursorPlan/@CursorActualType='Dynamic']\" mode=\"ToolTipDescription\">Cursor that can see all changes made by others.</xsl:template>\r\n  <xsl:template match=\"*[s:CursorPlan/@CursorActualType='Keyset']\" mode=\"ToolTipDescription\">Cursor that can see updates made by others, but not inserts.</xsl:template>\r\n  <xsl:template match=\"*[s:CursorPlan/@CursorActualType='SnapShot']\" mode=\"ToolTipDescription\">A cursor that does not see changes made by others.</xsl:template>\r\n\r\n  <!-- \r\n  ================================\r\n  Number handling\r\n  ================================\r\n  The following section contains templates used for handling numbers (scientific notation, rounding etc...)\r\n  -->\r\n\r\n  <!-- Outputs a number rounded to 7 decimal places - to be used for displaying all numbers.\r\n  This template accepts numbers in scientific notation. -->\r\n  <xsl:template name=\"round\">\r\n    <xsl:param name=\"value\" select=\"0\" />\r\n    <xsl:variable name=\"number\">\r\n      <xsl:call-template name=\"convertSciToNumString\">\r\n        <xsl:with-param name=\"inputVal\" select=\"$value\" />\r\n      </xsl:call-template>\r\n    </xsl:variable>\r\n    <xsl:value-of select=\"format-number(round(number($number) * 10000000) div 10000000, '0.#######')\" />\r\n  </xsl:template>\r\n  \r\n  <!-- Template for handling of scientific numbers\r\n  See: http://www.orm-designer.com/article/xslt-convert-scientific-notation-to-decimal-number -->\r\n  <xsl:variable name=\"max-exp\">\r\n    <xsl:value-of select=\"'0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000'\" />\r\n  </xsl:variable>\r\n\r\n  <xsl:template name=\"convertSciToNumString\">\r\n    <xsl:param name=\"inputVal\" select=\"0\" />\r\n\r\n    <xsl:variable name=\"numInput\">\r\n      <xsl:value-of select=\"translate(string($inputVal),'e','E')\" />\r\n    </xsl:variable>\r\n\r\n    <xsl:choose>\r\n      <xsl:when test=\"number($numInput) = $numInput\">\r\n        <xsl:value-of select=\"$numInput\" />\r\n      </xsl:when> \r\n      <xsl:otherwise>\r\n        <!-- ==== Mantisa ==== -->\r\n        <xsl:variable name=\"numMantisa\">\r\n          <xsl:value-of select=\"number(substring-before($numInput,'E'))\" />\r\n        </xsl:variable>\r\n\r\n        <!-- ==== Exponent ==== -->\r\n        <xsl:variable name=\"numExponent\">\r\n          <xsl:choose>\r\n            <xsl:when test=\"contains($numInput,'E+')\">\r\n              <xsl:value-of select=\"substring-after($numInput,'E+')\" />\r\n            </xsl:when>\r\n            <xsl:otherwise>\r\n              <xsl:value-of select=\"substring-after($numInput,'E')\" />\r\n            </xsl:otherwise>\r\n          </xsl:choose>\r\n        </xsl:variable>\r\n\r\n        <!-- ==== Coefficient ==== -->\r\n        <xsl:variable name=\"numCoefficient\">\r\n          <xsl:choose>\r\n            <xsl:when test=\"$numExponent > 0\">\r\n              <xsl:text>1</xsl:text>\r\n              <xsl:value-of select=\"substring($max-exp, 1, number($numExponent))\" />\r\n            </xsl:when>\r\n            <xsl:when test=\"$numExponent &lt; 0\">\r\n              <xsl:text>0.</xsl:text>\r\n              <xsl:value-of select=\"substring($max-exp, 1, -number($numExponent)-1)\" />\r\n              <xsl:text>1</xsl:text>\r\n            </xsl:when>\r\n            <xsl:otherwise>1</xsl:otherwise>\r\n          </xsl:choose>\r\n        </xsl:variable>\r\n        <xsl:value-of select=\"number($numCoefficient) * number($numMantisa)\" />\r\n      </xsl:otherwise>\r\n    </xsl:choose>\r\n  </xsl:template>\r\n</xsl:stylesheet>\r\n"

/***/ }
/******/ ])
});
;