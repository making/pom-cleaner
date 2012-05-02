extract versions in pom.xml, then replace placeholder and add property to <properties> !

example

from

    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.7</version>
        <scope>test</scope>
    </dependency>

to


    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>${junit.version}</version>
        <scope>test</scope>
    </dependency>

+

    <properties>
        ...
        <junit.version>4.7</junit.version>
    </properties>

## How To Build ##

$ mvn package

## How To Use ##

$ java -jar target/pom-cleaner.jar --help

$ java -jar target/pom-cleaner.jar <path to pom.xml>

$ java -jar target/pom-cleaner.jar <path to pom.xml> --overwrite


## License ##

Eclipse Public License - v 1.0



