package com.virtuslab.graph

import scala.collection.mutable

case class Graph[A, B](nodes: mutable.Set[Graph[A, B]#Node], edges: mutable.Set[Graph[A, B]#Edge]) {
  case class Node(content: A)

  case class Edge(
      from: Node,
      to: Node,
      property: B)

  def addNode(content: A): Unit = {
    nodes.add(Node(content))
  }

  def connect(
      from: A,
      to: A,
      properties: B
    ): Unit = {
    edges.add(Edge(Node(from), Node(to), properties))
  }
}
