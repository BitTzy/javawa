# LENDING LEDGER AND SAVING CIRCLE

## Milestone 1 desktop edition

This edition demonstrates the original milestone's registration, sign-in and proposal stage. The Java Swing window shows separate Lending and Borrowing lists. Users can propose a money loan or an item loan with a registered friend and view the pending terms from either participant's account. It runs as a standard user and opens a desktop window without a web server.

## Run in NetBeans

1. Extract the source ZIP to a writable folder such as Documents.
2. In NetBeans, select **File > Open Project** and open the folder containing `pom.xml`. Select JDK 21 for the project.
3. Select **Run Project**. NetBeans uses the included Maven action to start the desktop window. The first Maven build downloads its dependencies to the user account's Maven cache. [Maven projects in NetBeans](https://netbeans.apache.org/wiki/main/wiki/MavenBestPractices/)
4. Follow [the demonstration](PROGRESS.md).

With Maven and JDK 21 already available, you can also run `mvn spring-boot:run` from the project folder. With the separately supplied executable JAR, a standard user with JDK 21 can open the app from a writable folder with `java -jar LLSC-milestone-1-desktop.jar`.

## Run the demonstration on GitHub

1. Upload the extracted source files to the repository root, including the hidden `.github` folder. Choose a repository where you have write access and Actions is already enabled.
2. Commit the files. Open **Actions > Milestone 1** and choose the new run.
3. Expand **Demonstrate registration, sign-in and proposals**. The job registers sample accounts, verifies sign-in, saves and reads both types of proposal and checks access and validation. Share the completed run link with your professor.

The GitHub runner performs the demonstration and prints the results. Your professor can view the results while signed in with read access. The workflow uses only read permissions for the repository token. [GitHub workflow logs](https://docs.github.com/en/actions/how-tos/monitor-workflows/use-workflow-run-logs)

To run the same demonstration from source:

```sh
mvn package
java -jar target/lending-ledger-milestone-desktop.jar --demo
```

## Database

The desktop app creates `data/milestone.mv.db` in its working folder. H2 is embedded in the Java application. The original three-table schema is included in [schema.sql](src/main/resources/schema.sql). The packaged source contains neither account records nor database files. The automated demonstration uses an in-memory database populated with fictional accounts.
