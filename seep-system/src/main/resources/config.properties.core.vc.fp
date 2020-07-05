#-------------------------------------------------------------------------------
# Copyright (c) 2014 Imperial College London
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     Raul Castro Fernandez - initial API and implementation
#-------------------------------------------------------------------------------
######################
#INFRASTRUCTURE CONFIGURATION
######################
#mainAddr = 146.169.12.182
#mainAddr = 10.99.62.154
#coreMainAddr = 10.0.0.16
coreMainAddr = 172.16.1.2
useCoreAddr=true
#coreMainAddr = 10.0.0.10
#mainAddr = 127.0.0.1
#mainAddr = 192.168.0.107
mainAddr = 191.168.181.106
mainPort = 3500
ownPort = 3500
#separateControlNet=true
separateControlNet=false
sendDummyDownUpControlTraffic=false
sendDummyUpDownControlTraffic=false
sendDummyFailureControlTraffic=false
piggybackControlTraffic=true
#piggybackControlTraffic=false
mergeFailureAndRoutingCtrl=true
#mergeFailureAndRoutingCtrl=false
#enableUpstreamRoutingControl=true
enableUpstreamRoutingControl=false
disableBackpressureETX=false

######################
#GENERAL PARAMS
######################
baseId = 50
controlSocket = 50000
dataSocket = 40000
blindSocket = 60000
#inputQueueLength = 1000000
#inputQueueLength=100000
#inputQueueLength = 5
#inputQueueLength = 100
#inputQueueLength = 1000
inputQueueLength = 1
readyQueueLength = 10
#inputQueueLength = 10
boundReadyQueue=false

#####################
#ACK-WORKER PARAMS
#####################
ackWorkerActive = false 
ackEmitInterval = 3000

####################
#FAULT TOLERANCE PARAMS
####################
! Checkpointing mode: {large-state, light-state}
checkpointMode = light-state
parallelRecovery = true
eftMechanismEnabled = true
ftDiskMode=true
stateChunkSize=500000
! eliminate this thing. debugging
TTT=FALSE

######################
#MONITOR PARAMS
######################
monitorInterval = 5
monitorManagerPort = 5555
cpuUThreshold = 50
numMaxAlerts = 2
enableAutomaticScaleOut = true
minimumTimeBetweenSplit = 6
fileWithCpuU = OUT
minimumNodesAvailable = 10

######################
#BATCHING CONFIGURATION
######################
!batch tupleSize in bytes
tupleSize = 10
!packet size in bytes
packetSize = 16000
batchLimit = 1
!maximum latency allowed for a packet to be sent, in milliseconds
maxLatencyAllowed = 250

#######################
#SYSTEM ARCHITECTURE
# Do change this only if you know what you are doing
#######################
synchronousOutput = true
multicoreSupport = false

#####################
#DEBUGGING
####################
INIT=true

#####################
#MEANDER PARAMS
#####################
routeRecomputeIntervalSec=10
disableBackup=true
netAwareDispatcher=true
bestEffortAcks=true
enableFrontierRouting=true
autoReconnectChannel=true
reconnectBackoff=100
#frontierRouting=weightedRoundRobin
frontierRouting=backpressure
replicatedSinksHashRouting=backpressure
#frontierRouting=hash
#frontierRouting=shortestPath
#boundFrontierRoutingQueues=true
boundFrontierRoutingQueues=false
reliability=exactlyOnce
#reliability=bestEffort
#optimizeReplay=true
optimizeReplay=true
multiHopReplayOptimization=true
#multiHopReplayOptimization=false
#reliability=atLeastOnce
fctrlEmitInterval=1000
fctrlWorkerActive=true
periodicFctrlsOnly=true
noBufferSave=false
reprocessNonLocals=false
eagerPurgeOpQueue=false
scaleOutSinks=true
#scaleOutSinks=false
#barrierTimeout=2000
barrierTimeout=0
enableFailureCtrlWatchdog=true
#enableFailureCtrlWatchdog=false
#defaultProcessingDelay=100
defaultProcessingDelay=0
#defaultProcessingDelay=500
#srcOutputQueueTimestamps=true
srcOutputQueueTimestamps=false
enableGUI=false
enableSinkDisplay=false
routingCtrlDelay=1000
#routingCtrlDelay=500
#routingCtrlDelay=1000
#routingCtrlDelay=20000
enableLatencyBreakdown=false
initialPause=1000
#initialPause=200000
#initialPause=40000
#scheduledPauses=true
scheduledPauses=false
#costThreshold=6.0
costThreshold=40.0
costExponent=2.0
#costThreshold=5.1

#failureCtrlTimeout=4000
#failureCtrlTimeout=2000
failureCtrlTimeout=3000
retransmitTimeout=3000
trySendAlternativesTimeout=3000
downstreamsUnroutableTimeout=3000
socketConnectTimeout=10000

#failureCtrlTimeout=30000
#retransmitTimeout=29000
#trySendAlternativesTimeout=30000
#downstreamsUnroutableTimeout=30000


abortSessionTimeoutSec=300
#enableDownstreamsUnroutable=true
enableDownstreamsUnroutable=false
#enableBatchRetransmitTimeouts=true
enableBatchRetransmitTimeouts=false
enableHardReplay=true
requirePositiveAggregates=false

enableTupleTracking=true
#restrictRetransmitConstrained=true
restrictRetransmitConstrained=false
ctrlSocketBufSize=2048

####################
#MEANDER EXP PARAMS
####################
abortOnNodePoolEmpty=true
sendIndefinitely=true
#sendIndefinitely=false
#numTuples=20000
##numTuples=1000
#numTuples=100000
#numTuples=5000
#numTuples=10000
#warmUpTuples=250
warmUpTuples=50
#warmUpTuples=0
#warmUpTuples=0
#numTuples=86
#numTuples=1000
numTuples=500
#numTuples=1000
#numTuples=1000
#numTuples=100000
#numTuples=5000
#numTuples=50
#numTuples=9000
frameRate=1
#frameRate=24
#frameRate=50
#frameRate=48
#frameRate=15
#frameRate=55
recordImages=false
#resizeImages=true
resizeImages=false
#rateLimitSrc=true
rateLimitSrc=false
rateLimitConnections=false
#rateLimitConnections=true
#tupleSizeChars=1024
#tupleSizeChars=120000
#tupleSizeChars=12000
#tupleSizeChars=6000
tupleSizeChars=500
#tupleSizeChars=10
srcMaxBufferMB=20
socketBufferSize=16384
#maxTotalQueueSizeTuples=10000
maxTotalQueueSizeTuples=10
#maxSrcTotalQueueSizeTuples=10
#maxTotalQueueSizeTuples=1
#maxTotalQueueSizeTuples=100
#maxTotalQueueSizeTuples=1000
#maxTotalQueueSizeTuples=1
maxSrcTotalQueueSizeTuples=1
reportMaxSrcTotalQueueSizeTuples=false
skewLimit=0
#queryType=chain
#queryType=fdr
#queryType=fr
queryType=frJoin

##########################################################################
# Query specific params: TODO Have separate config.properties for query? #
##########################################################################
#preTrainedFaceRecModel=preTrainedFaceRecModel.xml
replicationFactor=1
#resourcesInJar=false
resourcesInJar=true
#repoDir=/data/dev/seep-github
repoDir=/home/dokeeffe/seep-ita
#repoDir=/home/pi/dev/seep-ita
#reorderImages=true
reorderImages=false
piAdHocDeployment=false
testFramesDir=images0.5
#testFramesDir=images
extraProps=../extraConfig.properties
sourceShutdownPause=300
ignoreQueueLengths=false
