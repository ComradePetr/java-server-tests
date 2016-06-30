@echo off

for /d %%i in (server-* timekeepers*) do (
	rd /S /Q %%i || exit
)
mkdir server-1 timekeepers || exit
cd server-1 || exit
start java -cp ../ServerTests.jar ru.spbau.mit.petrsmirnov.servertests.ServerMain || exit