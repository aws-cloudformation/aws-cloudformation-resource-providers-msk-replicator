# AWS::MSK::Replicator AmazonMskCluster

Details of an Amazon MSK cluster.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#mskclusterarn" title="MskClusterArn">MskClusterArn</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#mskclusterarn" title="MskClusterArn">MskClusterArn</a>: <i>String</i>
</pre>

## Properties

#### MskClusterArn

The ARN of an Amazon MSK cluster.

_Required_: Yes

_Type_: String

_Pattern_: <code>arn:(aws|aws-us-gov|aws-cn):kafka:.*</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

