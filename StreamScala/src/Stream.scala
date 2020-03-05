trait Streamable[DataType] {
  def filter(query: (DataType) => Boolean): Streamable[DataType]
  def map[NewValue](transform: (DataType) => NewValue): Streamable[NewValue]
  def flatMap[NewValue](transform: (DataType) => Streamable[NewValue]): Streamable[NewValue]
  def connect(whenEmission: (DataType) => Unit)(whenError: (Exception) => Unit): Connection
}


class Stream[DataType](val task: (Source[DataType]) => Connection) extends Streamable[DataType] {

  override def filter(query: DataType => Boolean): Streamable[DataType] = {
    new Stream[DataType]({ source =>
      connect(emission =>
        if (query(emission)) source.emit(emission)
        else ()) (error => source.error(error))
    })
  }

  override def map[NewValue](transform: DataType => NewValue): Streamable[NewValue] = {
    new Stream[NewValue]( { source =>
      connect(emission => source.emit(transform(emission))) (error => source.error(error))
    } )
  }

  override def flatMap[NewValue](transform: DataType => Streamable[NewValue]): Streamable[NewValue] = {
    new Stream[NewValue]({ source =>
      connect(emission => transform(emission)) (error => source.error(error))
    })
  }

  override def connect(whenEmission: DataType => Unit)(whenError: Exception => Unit): Connection = {
    task(new Source[DataType] {
      override def emit(emission: DataType): Unit = whenEmission(emission)

      override def error(error: Exception): Unit = whenError(error)
    })
  }

}

object main extends App {

  val stream = new Stream[Int]( { source =>
    source.emit(2)
    new Connection()
  }).filter(value => value >= 2)
    .map(value => value + "hello")
    .connect(x=>println(x))(error=>println(error))
}
