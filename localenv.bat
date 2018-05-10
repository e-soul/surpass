set BUILD_LIB=%~dp0build-lib

md %BUILD_LIB% 2> NUL

powershell -Command "& { Invoke-WebRequest -OutFile %BUILD_LIB%\junit.jar http://central.maven.org/maven2/junit/junit/4.12/junit-4.12.jar }"
powershell -Command "& { Invoke-WebRequest -OutFile %BUILD_LIB%\hamcrest-core.jar http://central.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar }"

set JUNIT_DEPS=%BUILD_LIB%\junit.jar;%BUILD_LIB%\hamcrest-core.jar
