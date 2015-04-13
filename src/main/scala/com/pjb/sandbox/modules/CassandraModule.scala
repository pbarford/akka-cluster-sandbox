package com.pjb.sandbox.modules

import com.datastax.driver.core.{Session, Cluster}

trait CassandraModule {
  this: ConfigModule =>

  private lazy val cluster:Cluster = {
    Cluster.builder().addContactPoints(cassandraContactPoints).build()
  }

  private lazy val session:Session = {
    cluster.connect(cassandraKeyspace)
  }

  def cassandraSession:() => Session = { () =>
    session
  }
}
