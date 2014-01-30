/*
 * Created on Nov 14, 2013 by Spyros Voulgaris
 *
 */
package peernet.util;

public class CountLatch
{
  int counter;

  public CountLatch(int k)
  {
    counter = k;
  }



  synchronized public void await()
  {
    try
    {
      while (counter > 0)
      {
        System.out.println("LATCH: Awaiting...");
        wait();
      }
      System.out.println("LATCH: ....released!");
    }
    catch (InterruptedException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }



  synchronized public void countDown()
  {
    if (--counter == 0)
      notifyAll();
  }



  synchronized public void countUp()
  {
    counter++;
  }
}
