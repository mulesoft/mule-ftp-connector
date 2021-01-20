FROM alpine:3.10
RUN apk --no-cache add vsftpd

COPY start_vsftpd.sh /bin/start_vsftpd.sh
COPY vsftpd.conf /etc/vsftpd/vsftpd.conf

RUN chmod +x /bin/start_vsftpd.sh

EXPOSE 21 21000-21010
VOLUME /ftp/ftp

RUN mkdir -p /ftp/ftp/data/
RUN mkdir -p /ftp/ftp/processed/

#TODO: mejorar esto:

COPY test1.xml /ftp/ftp/data/test1.xml
COPY test2.xml /ftp/ftp/data/test2.xml
COPY test3.xml /ftp/ftp/data/test3.xml

COPY test1.xml /ftp/ftp/data/test4.xml
COPY test2.xml /ftp/ftp/data/test5.xml
COPY test3.xml /ftp/ftp/data/test6.xml

COPY test1.xml /ftp/ftp/data/test7.xml
COPY test2.xml /ftp/ftp/data/test8.xml
COPY test3.xml /ftp/ftp/data/test9.xml

#COPY test1.xml /ftp/ftp/data/test10.xml
#COPY test2.xml /ftp/ftp/data/test11.xml
#COPY test3.xml /ftp/ftp/data/test12.xml

#COPY test1.xml /ftp/ftp/data/test13.xml
#COPY test2.xml /ftp/ftp/data/test14.xml
#COPY test3.xml /ftp/ftp/data/test15.xml

ENTRYPOINT ["/bin/start_vsftpd.sh"]