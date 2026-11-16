RENTPROOF - EXTERNAL LIBRARIES DIRECTORY
=========================================

Place your MySQL Connector JAR file here:
- Recommended: mysql-connector-j-8.0.33.jar (or any 8.x/9.x connector)

Download link:
https://dev.mysql.com/downloads/connector/j/
Or via Maven Central:
https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar

When placed here, you can run RentProof backend with:
javac -d bin (Get-ChildItem -Path backend/src/main/java -Filter *.java -Recurse).FullName
java -cp "bin;lib/*;backend/src/main/resources" com.rentproof.RentProofDemo
