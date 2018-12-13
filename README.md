# Akka Cluster up and running #

Bla bla bla ... ;-)

To make a node unavailable, first run the containers with NET_ADMIN capabilities:

``` bash
docker run --detach --cap-add NET_ADMIN ...
```

Then connect to a running container and block traffic:

```bash
docker exec -i -t ... bash
iptables -A INPUT -p tcp --dport 25520 -j DROP
```

To make a node available again:

```bash
iptables -D INPUT -p tcp --dport 25520 -j DROP
```

## On Minikube

``` bash
minikube start

# Start with 1 dynamic replica
kubectl apply -f k8s.yml
minikube service akkluster-http
http --stream $(minikube service --url akkluster-http)/events
http $(minikube service --url akkluster-management)/cluster/members

# Scale up to two replicas, down to one again and up to two again
kubectl patch deployment akkluster-dynamic -p '{"spec": {"replicas": 2}}'
kubectl patch deployment akkluster-dynamic -p '{"spec": {"replicas": 1}}'
kubectl patch deployment akkluster-dynamic -p '{"spec": {"replicas": 2}}'

# Make one dynamic replica unreachable
kubectl exec -i -t akkluster-dynamic-... bash 
iptables -A INPUT -p tcp -j DROP
# iptables -A INPUT -p tcp --dport 25520 -j DROP

# Scale up to three replicas and see what happens ...
kubectl patch deployment akkluster-dynamic -p '{"spec": {"replicas": 3}}'

# Make one dynamic replica leave
http --form PUT $(minikube service --url akkluster-management)/cluster/members/akka://akkluster@172.17.0.99:25520 'operation=Leave'

# Make unreachable dynamic replica reachable again
iptables -D INPUT -p tcp -j DROP
# iptables -D INPUT -p tcp --dport 25520 -j DROP
```

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with
any pull requests, please state that the contribution is your original work and that you license
the work to the project under the project's open source license. Whether or not you state this
explicitly, by submitting any copyrighted material via pull request, email, or other means you
agree to license the material under the project's open source license and warrant that you have the
legal authority to do so.

## License ##

This code is open source software licensed under the
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) license.
