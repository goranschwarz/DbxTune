In this directory you can create SQL Capture Normalizer/Resolver code!

A User Defined "nomalizer/rewriter" is where you can:
 - Take code which the SQL Capture normalizer parser and "rewrite" them so that the SQL is "valid" to the parser
 - Take code you know (which migt be long and dirty) and just use a simpler name for that (static normalization)
 
Since it's "your own logic" I needed som programing lunguage or scripting languare to support "extensive user defined logic"
I simply choosed Java as that language (for best inegration with DbxTune, since it's written in java)
Althow, you do not need to compile your classes into object classes/files and put them in a JAR file (or in the classpath)
Your User Defined Code (written in Java) is simply "compiled on-the-fly", this to emulate a "script" language.

Some extra information about this directory structure:
- If you want to create User Defined Narmalizations: The best way to put them is in the "normalizer-src/xxxTune" directory
- If your code has various Exceptions: A good place to store them is the "exceptions" directory

Check the files in: resources\normalizer-src\asetune for example code:
- DummyNormalizer.java.EXAMPLE         - This creates a "static" normalization
- DummyStatementFixer.java.EXAMPLE     - This rewrites a SQL Statement into some new SQL Code which can be handled by the Normalizer Parser

You can also check some code examples here: https://github.com/goranschwarz/DbxTune/tree/master/src/com/asetune/sql/norm

Good luck and you have any feedback, please send them to: goran_schwarz@hotmail.com
/Goran
