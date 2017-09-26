# Mongo Bench

Mongo Bench is a little and portable JAR that allow you to send customisable payload to a MongoDB cluster from any machine you want.

This is practical to determine the empirical limits of your MongoDB cluster.

The usual use case is to start a `mongostat` and a `htop` on your MongoDB server and run a benchmark on your application machines to see how many read or write operations per seconds your MongoDB can handle.
If you think the network is a bottleneck, maybe you could consider running this benchmark directly from the MongoDB server too to see the difference you get.
