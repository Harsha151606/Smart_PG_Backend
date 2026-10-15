FROM mysql:8.4.11

COPY mysql-recovery/reset-entrypoint.sh /usr/local/bin/railway-mysql-reset-entrypoint.sh
RUN chmod 0755 /usr/local/bin/railway-mysql-reset-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/railway-mysql-reset-entrypoint.sh"]
CMD ["mysqld"]
