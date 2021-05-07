FROM cimg/openjdk:8.0.275-browsers

RUN sudo apt-get update 
RUN sudo apt-get install -y openjdk-8-jre-headless locales 
RUN sudo sed -i -e 's/# \(en_US\.UTF-8 .*\)/\1/' /etc/locale.gen && \
    sudo locale-gen

# Downloading and installing Maven
# 1- Define a constant with the version of maven you want to install
ARG MAVEN_VERSION=3.6.3

# 2- Define a constant with the working directory
ARG USER_HOME_DIR="/root"

# 3- Define the SHA key to validate the maven download
ARG SHA=c35a1803a6e70a126e80b2b3ae33eed961f83ed74d18fcd16909b2d44d7dada3203f1ffe726c17ef8dcca2dcaa9fca676987befeadc9b9f759967a8cb77181c0

# 4- Define the URL where maven can be downloaded from
ARG BASE_URL=http://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries

# 5- Create the directories, download maven, validate the download, install it, remove downloaded file and set links
RUN sudo mkdir -p /usr/share/maven /usr/share/maven/ref \
  && echo "Downloading maven" \
  && sudo curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  \
  && echo "Checking download hash" \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  \
  && echo "Unziping maven" \
  && sudo tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  \
  && echo "Cleaning and setting links" \
  && sudo rm -f /tmp/apache-maven.tar.gz \
  && sudo ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

# 6- Define environmental variables required by Maven, like Maven_Home directory and where the maven repo is located
ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

COPY . /tmp
WORKDIR /tmp

# Jahia-cli is used to warmup the environment at startup
RUN PUPPETEER_SKIP_DOWNLOAD=true sudo npm install -g jahia-cli@0.0.47 --unsafe-perm

RUN sudo chmod 775 /tmp/env.run.sh

RUN sudo adduser --disabled-password jahians
RUN sudo chown -R jahians:jahians /tmp
RUN sudo chmod -R 775 /tmp
USER jahians

RUN mvn dependency:go-offline

CMD ["/bin/bash", "-c", "/tmp/env.run.sh"]
