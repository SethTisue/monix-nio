/*
 * Copyright (c) 2014-2017 by its authors. Some rights reserved.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.nio

import java.nio.ByteBuffer

import monix.eval.Callback
import monix.execution.Scheduler

package object tcp {
  /**
    * Returns a TCP socket [[monix.reactive.Observable Observable]] that can be subscribed to
    * in order to read the incoming bytes asynchronously.
    * It will close the socket on end-of-stream, signalling [[monix.execution.Ack.Stop Stop]]
    * after subscription or by cancelling it directly
    *
    * @param host hostname
    * @param port TCP port number
    * @param bufferSize the size of the buffer used for reading
    *
    * @return an [[monix.nio.tcp.AsyncSocketChannelObservable AsyncSocketChannelObservable]]
    */
  def readAsync(host: String, port: Int, bufferSize: Int = 256 * 1024) =
    new AsyncSocketChannelObservable(host, port, bufferSize)

  /**
    * Returns a TCP socket [[monix.reactive.Consumer Consumer]] that can be used
    * to send data asynchronously from an [[monix.reactive.Observable Observable]].
    * The underlying socket will be closed when the
    * [[monix.reactive.Observable Observable]] ends
    *
    * @param host hostname
    * @param port TCP port number
    *
    * @return an [[monix.nio.tcp.AsyncSocketChannelConsumer AsyncSocketChannelConsumer]]
    */
  def writeAsync(host: String, port: Int) =
    new AsyncSocketChannelConsumer(host, port)

  /**
    * Creates a TCP client - an async reader([[monix.nio.tcp.AsyncSocketChannelObservable AsyncSocketChannelObservable]])
    * and an async writer([[monix.nio.tcp.AsyncSocketChannelConsumer AsyncSocketChannelConsumer]]) pair
    * that both are using the same underlying socket.
    * The reader will be the one in charge of closing the underlying socket by
    * signalling [[monix.execution.Ack.Stop Stop]] after subscription or by cancelling it directly,
    * if no `end-of-stream` is received
    *
    * @param host hostname
    * @param port TCP port number
    * @param bufferSize the size of the buffer used for reading
    * @return an [[monix.nio.tcp.AsyncTcpClient AsyncTcpClient]]
    */
  def readWriteAsync(
    host: String,
    port: Int,
    bufferSize: Int = 256 * 1024
  )(implicit scheduler: Scheduler): AsyncTcpClient = AsyncTcpClient(host, port, bufferSize)

  /**
    * Creates a TCP server
    *
    * @param host hostname
    * @param port TCP port number
    * @return an [[monix.nio.tcp.AsyncTcpServer AsyncTcpServer]]
    */
  def asyncServer(
    host: String,
    port: Int
  )(implicit scheduler: Scheduler) = AsyncTcpServer(host, port).tcpServer

  private[tcp] def asyncChannelWrapper(asyncSocketChannel: AsyncSocketChannel, closeWhenDone: Boolean) = new AsyncChannel {
    override def read(dst: ByteBuffer, position: Long, callback: Callback[Int]): Unit =
      asyncSocketChannel.read(dst, callback)

    override def write(b: ByteBuffer, position: Long, callback: Callback[Int]): Unit =
      asyncSocketChannel.write(b, callback)

    override def close(): Unit =
      asyncSocketChannel.close()

    override def closeOnComplete(): Boolean = closeWhenDone
  }
}
