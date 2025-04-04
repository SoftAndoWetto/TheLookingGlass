@echo off
call mvnw.cmd clean install
mvnw.cmd exec:java
