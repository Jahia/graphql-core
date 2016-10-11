# graphql-core-feature
This project contains a Karaf feature to integrate a GraphQL server

## About this project

This project makes it possible to build GraphQL backends to expose services and content using the GraphQL interfaces.

## Building & installing

1. Clone this project : 

        https://github.com/sergehuber/graphql-java-servlet.git
        
2. Switch to the `spec-updates` branch 
        
        git checkout spec-updates
        
3. Build and install the package to your local maven repository using the command:

        ./gradlew clean install        
    
4. Build this project (graphql-core-feature) using :

        mvn clean install
    
5. Inside DX, connect to the SSH Karaf shell using :

        ssh -p 8101 TOOLS_USER@localhost
            
    where TOOLS_USER and the associated password are the passwords you setup during the installation of DX for the system
    tools
   
6. Enter the following command : 

        feature:repo-add mvn:org.jahia.modules/graphql-core/1.0-SNAPSHOT/xml/features
    
7. Install the feature using : 

        feature:install graphql-core

## Deploying modifications

If you want to deploy again to test modifications simply use : 

1. `mvn clean install`
2. `ssh -p 8101 TOOLS_USER@localhost`
3. `feature:repo-refresh`
4. `feature:install graphql-core`

The `feature:install` command will indeed install updates.