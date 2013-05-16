package org.elasticmq.rest.sqs

import org.elasticmq.MillisVisibilityTimeout

import Constants._
import org.joda.time.Duration
import org.elasticmq.msg.{UpdateQueueDelay, UpdateQueueDefaultVisibilityTimeout, GetQueueStatistics}
import org.elasticmq.actor.reply._
import scala.concurrent.Future
import org.elasticmq.rest.sqs.directives.ElasticMQDirectives

trait QueueAttributesDirectives { this: ElasticMQDirectives with AttributesModule =>
  object QueueWriteableAttributeNames {
    val VisibilityTimeoutAttribute = "VisibilityTimeout"
    val DelaySecondsAttribute = "DelaySeconds"

    val AllWriteableAttributeNames = VisibilityTimeoutAttribute :: DelaySecondsAttribute :: Nil
  }

  object QueueReadableAttributeNames {
    val ApproximateNumberOfMessagesAttribute = "ApproximateNumberOfMessages"
    val ApproximateNumberOfMessagesNotVisibleAttribute = "ApproximateNumberOfMessagesNotVisible"
    val ApproximateNumberOfMessagesDelayedAttribute = "ApproximateNumberOfMessagesDelayed"
    val CreatedTimestampAttribute = "CreatedTimestamp"
    val LastModifiedTimestampAttribute = "LastModifiedTimestamp"

    val AllAttributeNames = QueueWriteableAttributeNames.AllWriteableAttributeNames ++
      (ApproximateNumberOfMessagesAttribute ::
        ApproximateNumberOfMessagesNotVisibleAttribute ::
        ApproximateNumberOfMessagesDelayedAttribute ::
        CreatedTimestampAttribute ::
        LastModifiedTimestampAttribute :: Nil)
  }

  val getQueueAttributes = {
    action("GetQueueAttributes") {
      queueActorAndDataFromPath { (queueActor, queueData) =>
        anyParamsMap { parameters =>
          import QueueWriteableAttributeNames._
          import QueueReadableAttributeNames._

          def calculateAttributeValues(attributeNames: List[String]): List[(String, Future[String])] = {
            lazy val stats = queueActor ? GetQueueStatistics(System.currentTimeMillis())

            import AttributeValuesCalculator.Rule

            attributeValuesCalculator.calculate(attributeNames,
              Rule(VisibilityTimeoutAttribute, () => Future.successful(queueData.defaultVisibilityTimeout.seconds.toString)),
              Rule(DelaySecondsAttribute, () => Future.successful(queueData.delay.getStandardSeconds.toString)),
              Rule(ApproximateNumberOfMessagesAttribute, () => stats.map(_.approximateNumberOfVisibleMessages.toString)),
              Rule(ApproximateNumberOfMessagesNotVisibleAttribute, () => stats.map(_.approximateNumberOfInvisibleMessages.toString)),
              Rule(ApproximateNumberOfMessagesDelayedAttribute, () => stats.map(_.approximateNumberOfMessagesDelayed.toString)),
              Rule(CreatedTimestampAttribute, () => Future.successful((queueData.created.getMillis/1000L).toString)),
              Rule(LastModifiedTimestampAttribute, () => Future.successful((queueData.lastModified.getMillis/1000L).toString)))
          }

          def responseXml(attributes: List[(String, String)]) = {
            <GetQueueAttributesResponse>
              <GetQueueAttributesResult>
                {attributesToXmlConverter.convert(attributes)}
              </GetQueueAttributesResult>
              <ResponseMetadata>
                <RequestId>{EmptyRequestId}</RequestId>
              </ResponseMetadata>
            </GetQueueAttributesResponse>
          }

          val attributeNames = attributeNamesReader.read(parameters, AllAttributeNames)
          val attributesFuture = Future.sequence(calculateAttributeValues(attributeNames).map(p => p._2.map((p._1, _))))

          attributesFuture.map { attributes =>
            respondWith {
              responseXml(attributes)
            }
          }
        }
      }
    }
  }

  val setQueueAttributes = {
    action("SetQueueAttributes") {
      queueActorFromPath { queueActor =>
        anyParamsMap { parameters =>
          val attributes = attributeNameAndValuesReader.read(parameters)

          val result = attributes.map({ case (attributeName, attributeValue) =>
            attributeName match {
              case QueueWriteableAttributeNames.VisibilityTimeoutAttribute => {
                queueActor ? UpdateQueueDefaultVisibilityTimeout(MillisVisibilityTimeout.fromSeconds(attributeValue.toLong))
              }
              case QueueWriteableAttributeNames.DelaySecondsAttribute => {
                queueActor ? UpdateQueueDelay(Duration.standardSeconds(attributeValue.toLong))
              }
              case _ => Future(throw new SQSException("InvalidAttributeName"))
            }
          })

          Future.sequence(result).map { _ =>
            respondWith {
              <SetQueueAttributesResponse>
                <ResponseMetadata>
                  <RequestId>{EmptyRequestId}</RequestId>
                </ResponseMetadata>
              </SetQueueAttributesResponse>
            }
          }
        }
      }
    }
  }
}