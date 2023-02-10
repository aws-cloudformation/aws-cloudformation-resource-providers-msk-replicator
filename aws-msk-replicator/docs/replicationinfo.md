# AWS::MSK::Replicator ReplicationInfo

Specifies configuration for replication between a source and target Kafka cluster.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#sourcekafkaclusterarn" title="SourceKafkaClusterArn">SourceKafkaClusterArn</a>" : <i>String</i>,
    "<a href="#targetkafkaclusterarn" title="TargetKafkaClusterArn">TargetKafkaClusterArn</a>" : <i>String</i>,
    "<a href="#targetcompressiontype" title="TargetCompressionType">TargetCompressionType</a>" : <i>String</i>,
    "<a href="#topicreplication" title="TopicReplication">TopicReplication</a>" : <i><a href="topicreplication.md">TopicReplication</a></i>,
    "<a href="#consumergroupreplication" title="ConsumerGroupReplication">ConsumerGroupReplication</a>" : <i><a href="consumergroupreplication.md">ConsumerGroupReplication</a></i>
}
</pre>

### YAML

<pre>
<a href="#sourcekafkaclusterarn" title="SourceKafkaClusterArn">SourceKafkaClusterArn</a>: <i>String</i>
<a href="#targetkafkaclusterarn" title="TargetKafkaClusterArn">TargetKafkaClusterArn</a>: <i>String</i>
<a href="#targetcompressiontype" title="TargetCompressionType">TargetCompressionType</a>: <i>String</i>
<a href="#topicreplication" title="TopicReplication">TopicReplication</a>: <i><a href="topicreplication.md">TopicReplication</a></i>
<a href="#consumergroupreplication" title="ConsumerGroupReplication">ConsumerGroupReplication</a>: <i><a href="consumergroupreplication.md">ConsumerGroupReplication</a></i>
</pre>

## Properties

#### SourceKafkaClusterArn

Amazon Resource Name of the source Kafka cluster.

_Required_: Yes

_Type_: String

_Pattern_: <code>arn:(aws|aws-us-gov|aws-cn):kafka:.*</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TargetKafkaClusterArn

Amazon Resource Name of the target Kafka cluster.

_Required_: Yes

_Type_: String

_Pattern_: <code>arn:(aws|aws-us-gov|aws-cn):kafka:.*</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TargetCompressionType

The type of compression to use writing records to target Kafka cluster.

_Required_: Yes

_Type_: String

_Allowed Values_: <code>NONE</code> | <code>GZIP</code> | <code>SNAPPY</code> | <code>LZ4</code> | <code>ZSTD</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TopicReplication

_Required_: Yes

_Type_: <a href="topicreplication.md">TopicReplication</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ConsumerGroupReplication

Configuration relating to consumer group replication.

_Required_: Yes

_Type_: <a href="consumergroupreplication.md">ConsumerGroupReplication</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

