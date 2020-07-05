import os

from core.service import CoreService, addservice
from core.misc.ipaddr import IPv4Prefix, IPv6Prefix

def build_cfg(lwid, cls, node, filename, services):
    if filename is 'shutdown.sh':
        cfg = "#!/bin/sh\n"
        cfg += "# auto-generated by FrontierWorker%d (worker%d.py)\n"%(lwid,lwid)
        cfg = "touch worker%d.shutdown\n"%lwid
        return cfg
    else:
        #repo_dir = "/data/dev/seep-github"
        repo_dir = "%s/../../../../../.."%os.path.dirname(os.path.realpath(__file__))
        seep_example_dir = "%s/seep-system/examples/acita_demo_2015"%repo_dir
        #seep_jar = "seep-system-0.0.1-SNAPSHOT.jar"
        #lib_dir = "lib"
        log_dir = "log%d"%lwid

        cfg = "#!/bin/sh\n"
        cfg += "# auto-generated by FrontierWorker%d (worker%d.py)\n"%(lwid,lwid)
        #cfg += "mkdir %s\n"%lib_dir
        cfg += "mkdir %s\n"%log_dir
        cfg += "pwd\n"
        cfg += "ls\n"
        cfg += "'echo parent directory:'\n"
        cfg += "ls ..\n"
        cfg += "cp %s/core-emane/vldb/config/run-worker.py .\n"%(seep_example_dir)
        if filter(lambda service: service._name == "OLSRETX", services):
            cfg += "cp %s/core-emane/vldb/config/olsrd-net-rates.sh net-rates.sh\n"%(seep_example_dir)
            cfg += "cp %s/core-emane/vldb/config/olsrd-net-topology.sh net-topology.sh\n"%(seep_example_dir)
            cfg += "cp %s/core-emane/vldb/config/olsrd-all-net-info.sh all-net-info.sh\n"%(seep_example_dir)
            cfg += "cp %s/core-emane/vldb/config/olsrd-get-neighbours.sh get-neighbours.sh\n"%(seep_example_dir)
            cfg += "cp %s/core-emane/vldb/config/tcp-rto-info.sh tcp-rto-info.sh\n"%(seep_example_dir)
        else:
            cfg += "cp %s/core-emane/vldb/config/net-rates.sh net-rates.sh\n"%(seep_example_dir)
            cfg += "cp %s/core-emane/vldb/config/net-topology1.sh net-topology.sh\n"%(seep_example_dir)
            cfg += "cp %s/core-emane/vldb/config/all-net-info.sh all-net-info.sh\n"%(seep_example_dir)
            cfg += "cp %s/core-emane/vldb/config/tcp-rto-info.sh tcp-rto-info.sh\n"%(seep_example_dir)
        #cfg += "cp %s/lib/%s %s\n"%(seep_example_dir, seep_jar, lib_dir)
        cfg += 'echo "Starting FrontierWorker%d on `hostname`(`hostname -i`)"\n'%lwid
        cfg += 'echo "cating /etc/hosts"\n'
        cfg += 'cat /etc/hosts\n'
        cfg += "ip route > rt.log\n"
        cfg += "ifconfig > if.log\n"
        cfg += "/sbin/route > rts.log\n"
        cfg += 'echo "0.0,0.0,0.0" > node.xyz\n'
        cfg += "./net-rates.sh\n"
        cfg += "./get-neighbours.sh %s &\n"%(node.objid)
        worker_processors = ",".join(map(str, range(3,64,4)))
        #cfg += "taskset -c %s ./run-worker.py --id %d\n"%(worker_processors, lwid)
        #cfg += "taskset -c 25-31 ./run-worker.py --id %d\n"%lwid
        cfg += "./run-worker.py --id %d\n"%lwid
        cfg += "/sbin/route > rts-end.log\n"
        cfg += "touch worker%d.shutdown\n"%lwid
        #cfg += "java -jar %s/%s Worker\n"%(lib_dir, seep_jar)

        return cfg
