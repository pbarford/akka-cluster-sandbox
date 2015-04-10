package com.pjb.sandbox.util


import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object Merger {
  private val keysFor = Map("markets" -> "id", "selections" -> "id", "indexResults" -> "indexValue")

  private val typeRef = new TypeReference[Map[String, AnyRef]] {}
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def toMap(data: String): Map[String, AnyRef] = {
    mapper.readValue(data, typeRef)
  }

  def toJson(map: Map[String, AnyRef]): String = {
    mapper.writeValueAsString(map)
  }

  def merge(source: Map[String, AnyRef],
            update: Map[String, AnyRef]): Map[String, AnyRef] = {

    def inner(m1: Map[String,AnyRef],
              m2:Map[String,AnyRef],
              common:Set[String],
              distinct:Set[String]):Map[String, AnyRef] = {


      //val distinctUpdates = distinct.filter(e => e != "action").map {element =>
      val distinctUpdates = distinct.map {element =>
        (m1.get(element), m2.get(element)) match {
          case (None,Some(v2)) =>
            element -> v2
          case (Some(v1),None) =>
            element -> v1
          case (Some(v1),Some(v2)) =>
            element -> v2
        }
      }
      //val commonUpdates = common.filter(e => e != "action").map { element =>
      val commonUpdates = common.map { element =>
        (m1.get(element) ,m2.get(element)) match {
          case (Some(v1: Map[String, AnyRef]), Some(v2: Map[String, AnyRef])) =>
            element -> inner(v1, v2, v1.keySet.intersect(v2.keySet), v1.keySet.diff(v2.keySet) ++ v2.keySet.diff(v1.keySet))

          case (Some(v1: List[Map[String, AnyRef]]),Some(v2: List[Map[String, AnyRef]])) =>

            val keyForElement = keysFor.getOrElse(element, "id")
            val ids1 = v1.groupBy(_.get(keyForElement))
            val ids2 = v2.groupBy(_.get(keyForElement))

            val commonIds = ids1.keySet.intersect(ids2.keySet)
            val distinctIds = ids2.keySet.diff(ids1.keySet) ++ ids1.keySet.diff(ids2.keySet)

            val vv1 = v1.filter(e => commonIds.contains(e.get(keyForElement)))
            val vv2 = v2.filter(e => commonIds.contains(e.get(keyForElement)))
            val nonProcessed = v1.filter(e => distinctIds.contains(e.get(keyForElement))) ++
              v2.filter(e => distinctIds.contains(e.get(keyForElement)))

            val processed = vv1.map { m1 =>
              val m2 = findMatchingMapsByKey(keyForElement, m1, vv2)
              inner(m1, m2,
                m1.keySet.intersect(m2.keySet),
                m1.keySet.diff(m2.keySet) ++ m2.keySet.diff(m1.keySet)
              )
            } ++ nonProcessed

            element -> processed

          case (Some(v1),Some(v2)) =>
            element -> v2
          case (None,Some(v2)) =>
            element -> v2
          case (Some(v1),None) =>
            element -> v1

        }
      }

      distinctUpdates ++ commonUpdates

    }.toMap

    inner(source, update, source.keySet.intersect(update.keySet), source.keySet.diff(update.keySet) ++ update.keySet.diff(source.keySet))
  }

  private def findMatchingMapsByKey(key:String, source:Map[String,AnyRef], search :List[Map[String, AnyRef]]):Map[String,AnyRef] ={
    search.find(m => m.get(key) == source.get(key)).get
  }

}

