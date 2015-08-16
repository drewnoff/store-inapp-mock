# Version: 0.1
FROM ubuntu:14.04
MAINTAINER Romanov Andrey "drewnoff@yandex.com"

# Packages
RUN echo "deb mirror://mirrors.ubuntu.com/mirrors.txt utopic main restricted universe multiverse \n\
    deb mirror://mirrors.ubuntu.com/mirrors.txt utopic-updates main restricted universe multiverse \n\
    deb mirror://mirrors.ubuntu.com/mirrors.txt utopic-backports main restricted universe multiverse \n\
    deb mirror://mirrors.ubuntu.com/mirrors.txt utopic-security main restricted universe multiverse" > /etc/apt/sources.list.d/all-mirrors.list
RUN apt-get update && \
    apt-get -y install --no-install-recommends openjdk-7-jdk mongodb && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# MongoDB files
RUN mkdir -p /data/db
VOLUME ["/data/db"]

# Service
ADD target/scala-2.10/store-in-app-mock-service-assembly-1.0.jar /ss.jar

# Default command
ENV DB_DBNAME inapps
ENV DB_COLLECTION inapps
ADD run.sh /run.sh
RUN chmod +x /run.sh
CMD ["/run.sh"]

EXPOSE 8080