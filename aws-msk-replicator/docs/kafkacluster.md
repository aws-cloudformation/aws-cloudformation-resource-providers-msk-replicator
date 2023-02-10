# AWS::MSK::Replicator KafkaCluster

Details of a Kafka cluster for replication.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#amazonmskcluster" title="AmazonMskCluster">AmazonMskCluster</a>" : <i><a href="amazonmskcluster.md">AmazonMskCluster</a></i>,
    "<a href="#vpcconfig" title="VpcConfig">VpcConfig</a>" : <i><a href="kafkaclusterclientvpcconfig.md">KafkaClusterClientVpcConfig</a></i>
}
</pre>

### YAML

<pre>
<a href="#amazonmskcluster" title="AmazonMskCluster">AmazonMskCluster</a>: <i><a href="amazonmskcluster.md">AmazonMskCluster</a></i>
<a href="#vpcconfig" title="VpcConfig">VpcConfig</a>: <i><a href="kafkaclusterclientvpcconfig.md">KafkaClusterClientVpcConfig</a></i>
</pre>

## Properties

#### AmazonMskCluster

Details of an Amazon MSK cluster.

_Required_: Yes

_Type_: <a href="amazonmskcluster.md">AmazonMskCluster</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VpcConfig

Details of an Amazon VPC which has network connectivity to the Kafka cluster.

_Required_: Yes

_Type_: <a href="kafkaclusterclientvpcconfig.md">KafkaClusterClientVpcConfig</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

