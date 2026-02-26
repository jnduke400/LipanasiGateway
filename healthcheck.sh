#!/bin/sh
if mysqladmin ping -h localhost -uroot -p"$(cat /run/secrets/root_db_password)"; then
  exit 0
else
  exit 1
fi