package presenters

import play.api._
import org.joda.time._

import models._

case class MessagePresenter(msg: Message) extends AnyVal {

  def author: String = msg.author.name

  def date: String = msg.date.toString("HH:mm")

  def id: String = msg.id.toString

  def text: String = msg.text

  def avatar: String = "http://lorempixel.com/32/32/"

}