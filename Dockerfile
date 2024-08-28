#
# COPYRIGHT Ericsson 2023
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

FROM armdocker.rnd.ericsson.se/proj-esoa-so/so-base-openjdk17:1.3.2-1
RUN zypper --non-interactive install --no-recommends python311 \
        && ln -s /usr/bin/python3.11 /usr/bin/python3 \
        && zypper --non-interactive install --no-recommends python311-pip \
        && pip3 install -q requests xmlToDict==0.13.0 lxml==5.1.0 openpyxl==3.1.2 pandas==2.2.0 PyYAML==6.0.1 yangify==0.1.3
COPY eric-esoa-dr-python/target/*.whl .
RUN pip3 install *.whl
# Soft link restricted set of shell commands to /usr/local/bin. D&R users will be restricted to executing this set of commands.
RUN for binary in awk base64 curl cut date echo egrep gawk grep python3 sed sort sleep; do ln -s /usr/bin/${binary} /usr/local/bin/${binary};done
COPY eric-esoa-dr-service-jar/target/eric-esoa-dr-service-jar.jar eric-esoa-dr-service.jar
COPY set_log_level.sh /set_log_level.sh
COPY entryPoint.sh /entryPoint.sh
EXPOSE 8080
ENV JAVA_OPTS ""
ENV LOADER_PATH ""

ARG user=appuser
ARG uid=117402
ARG gid=117402

RUN echo "${user}:x:${uid}:${gid}:UI-user:/:/bin/bash" >> /etc/passwd \
    && sed -i 's/^\(root.*\):.*/\1:\/bin\/false/' /etc/passwd && sed -i 's/^root:/root:!/' /etc/shadow

RUN chmod +x /entryPoint.sh
ENTRYPOINT ["sh", "-c", "/entryPoint.sh $JAVA_OPTS"]
