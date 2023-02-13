# AWS::MSK::Replicator ConsumerGroupReplication

Configuration relating to consumer group replication.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#consumergroupstoreplicate" title="ConsumerGroupsToReplicate">ConsumerGroupsToReplicate</a>" : <i>[ String, ... ]</i>,
    "<a href="#consumergroupstoexclude" title="ConsumerGroupsToExclude">ConsumerGroupsToExclude</a>" : <i>[ String, ... ]</i>,
    "<a href="#synchroniseconsumergroupoffsets" title="SynchroniseConsumerGroupOffsets">SynchroniseConsumerGroupOffsets</a>" : <i>Boolean</i>,
    "<a href="#detectandcopynewconsumergroups" title="DetectAndCopyNewConsumerGroups">DetectAndCopyNewConsumerGroups</a>" : <i>Boolean</i>
}
</pre>

### YAML

<pre>
<a href="#consumergroupstoreplicate" title="ConsumerGroupsToReplicate">ConsumerGroupsToReplicate</a>: <i>
      - String</i>
<a href="#consumergroupstoexclude" title="ConsumerGroupsToExclude">ConsumerGroupsToExclude</a>: <i>
      - String</i>
<a href="#synchroniseconsumergroupoffsets" title="SynchroniseConsumerGroupOffsets">SynchroniseConsumerGroupOffsets</a>: <i>Boolean</i>
<a href="#detectandcopynewconsumergroups" title="DetectAndCopyNewConsumerGroups">DetectAndCopyNewConsumerGroups</a>: <i>Boolean</i>
</pre>

## Properties

#### ConsumerGroupsToReplicate

List of regular expression patterns indicating the consumer groups to copy.

_Required_: Yes

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ConsumerGroupsToExclude

List of regular expression patterns indicating the consumer groups that should not be replicated.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SynchroniseConsumerGroupOffsets

Whether to periodically write the translated offsets to __consumer_offsets topic in target cluster.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DetectAndCopyNewConsumerGroups

Whether to periodically check for new consumer groups.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

