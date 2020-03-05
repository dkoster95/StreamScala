//class Source {
//
//}

trait Source[DataType] {
  def emit(emission: DataType)
  def error(error: Exception)
}
