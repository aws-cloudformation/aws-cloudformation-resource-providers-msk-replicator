# AWS::MSK::Replicator

Resource Type definition for AWS::MSK::Replicator

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::MSK::Replicator",
    "Properties" : {
        "<a href="#replicatorname" title="ReplicatorName">ReplicatorName</a>" : <i>String</i>,
        "<a href="#currentversion" title="CurrentVersion">CurrentVersion</a>" : <i>String</i>,
        "<a href="#description" title="Description">Description</a>" : <i>String</i>,
        "<a href="#kafkaclusters" title="KafkaClusters">KafkaClusters</a>" : <i>[ <a href="kafkacluster.md">KafkaCluster</a>, ... ]</i>,
        "<a href="#replicationinfolist" title="ReplicationInfoList">ReplicationInfoList</a>" : <i>[ <a href="replicationinfo.md">ReplicationInfo</a>, ... ]</i>,
        "<a href="#serviceexecutionrolearn" title="ServiceExecutionRoleArn">ServiceExecutionRoleArn</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::MSK::Replicator
Properties:
    <a href="#replicatorname" title="ReplicatorName">ReplicatorName</a>: <i>String</i>
    <a href="#currentversion" title="CurrentVersion">CurrentVersion</a>: <i>String</i>
    <a href="#description" title="Description">Description</a>: <i>String</i>
    <a href="#kafkaclusters" title="KafkaClusters">KafkaClusters</a>: <i>
      - <a href="kafkacluster.md">KafkaCluster</a></i>
    <a href="#replicationinfolist" title="ReplicationInfoList">ReplicationInfoList</a>: <i>
      - <a href="replicationinfo.md">ReplicationInfo</a></i>
    <a href="#serviceexecutionrolearn" title="ServiceExecutionRoleArn">ServiceExecutionRoleArn</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
</pre>

## Properties

#### ReplicatorName

The name of the replicator.

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>128</code>

_Pattern_: <code>^[0-9A-Za-z][0-9A-Za-z-]{0,}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### CurrentVersion

The current version of the MSK replicator.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Description

A summary description of the replicator.

_Required_: No

_Type_: String

_Maximum Length_: <code>1024</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### KafkaClusters

Specifies a list of Kafka clusters which are targets of the replicator.

_Required_: Yes

_Type_: List of <a href="kafkacluster.md">KafkaCluster</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### ReplicationInfoList

A list of replication configurations, where each configuration targets a given source cluster to target cluster replication flow.

_Required_: Yes

_Type_: List of <a href="replicationinfo.md">ReplicationInfo</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ServiceExecutionRoleArn

The Amazon Resource Name (ARN) of the IAM role used by the replicator to access external resources.

_Required_: Yes

_Type_: String

_Pattern_: <code>arn:(aws|aws-us-gov|aws-cn):iam:.*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

A collection of tags associated with a resource

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the ReplicatorArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### ReplicatorArn

Amazon Resource Name for the created replicator.

