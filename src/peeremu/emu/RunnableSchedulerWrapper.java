/*
 * Created on Apr 3, 2005 by Spyros Voulgaris
 *
 */
package peeremu.emu;

/**
 * @author Spyros Voulgaris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RunnableSchedulerWrapper implements Runnable
{
  Runnable command;
  RunnableScheduler sched;

  public RunnableSchedulerWrapper(Runnable command, RunnableScheduler sched)
  {
    this.command = command;
    this.sched = sched;
  }


  public void run()
  {
    if (RunnableObserver.class.isAssignableFrom(command.getClass()))
    {
      System.out.print('.');
      System.out.flush();
    }

    if (sched.active())
    {
      command.run();
      if (RunnableObserver.class.isAssignableFrom(command.getClass()))
      {
        System.out.print('#');
        System.out.flush();
      }
    }
    else
      System.out.println("blocked");
  }

  public Object clone() throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException(
        "Clone not supported for RunnableSchedulerWrapper");
  }
}
