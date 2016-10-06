# graphql-core-feature
This project contains a Karaf feature to integrate a GraphQL server

## Building & installing

1. Clone this repository : 

    git clone https://github.com/sergehuber/graphql-java-servlet.git
    
2. Build and install the forked graphql-java-servlet project : 

    ./gradlew clean install
    
3. Build this project (graphql-core-feature) using :

    mvn clean install
    
4. Inside DX, connect to the SSH Karaf shell using :

    ssh -p 8101 TOOLS_USER@localhost
    
   where TOOLS_USER and the associated password are the passwords you setup during the installation of DX for the system
   tools
   
5. Enter the following command : 

    feature:repo-add mvn:org.jahia.modules/graphql-core/1.0-SNAPSHOT/xml/features
    
6. Install the feature using : 

    feature:install graphql-core
