Testing
=======

To run a dockerized Telegraf instance that receives InfluxDB line protocol on
http://localhost:8086 and prints it out on stdout run:

```
docker run -p 8086:8086 -v $PWD/telegraf.conf:/etc/telegraf/telegraf.conf:ro telegraf
```
