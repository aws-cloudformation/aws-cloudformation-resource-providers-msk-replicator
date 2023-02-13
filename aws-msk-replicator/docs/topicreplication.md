# AWS::MSK::Replicator TopicReplication

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#topicstoreplicate" title="TopicsToReplicate">TopicsToReplicate</a>" : <i>[ String, ... ]</i>,
    "<a href="#topicstoexclude" title="TopicsToExclude">TopicsToExclude</a>" : <i>[ String, ... ]</i>,
    "<a href="#copytopicconfigurations" title="CopyTopicConfigurations">CopyTopicConfigurations</a>" : <i>Boolean</i>,
    "<a href="#copyaccesscontrollistsfortopics" title="CopyAccessControlListsForTopics">CopyAccessControlListsForTopics</a>" : <i>Boolean</i>,
    "<a href="#detectandcopynewtopics" title="DetectAndCopyNewTopics">DetectAndCopyNewTopics</a>" : <i>Boolean</i>
}
</pre>

### YAML

<pre>
<a href="#topicstoreplicate" title="TopicsToReplicate">TopicsToReplicate</a>: <i>
      - String</i>
<a href="#topicstoexclude" title="TopicsToExclude">TopicsToExclude</a>: <i>
      - String</i>
<a href="#copytopicconfigurations" title="CopyTopicConfigurations">CopyTopicConfigurations</a>: <i>Boolean</i>
<a href="#copyaccesscontrollistsfortopics" title="CopyAccessControlListsForTopics">CopyAccessControlListsForTopics</a>: <i>Boolean</i>
<a href="#detectandcopynewtopics" title="DetectAndCopyNewTopics">DetectAndCopyNewTopics</a>: <i>Boolean</i>
</pre>

## Properties

#### TopicsToReplicate

List of regular expression patterns indicating the topics to copy.

_Required_: Yes

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TopicsToExclude

List of regular expression patterns indicating the topics that should not be replicated.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### CopyTopicConfigurations

Whether to periodically configure remote topics to match their corresponding upstream topics.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### CopyAccessControlListsForTopics

Whether to periodically configure remote topic ACLs to match their corresponding upstream topics.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DetectAndCopyNewTopics

Whether to periodically check for new topics and partitions.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

