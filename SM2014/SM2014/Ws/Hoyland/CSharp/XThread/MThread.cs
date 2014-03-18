﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;

namespace Ws.Hoyland.CSharp.XThread
{
    internal class MThread
    {
        private Thread t;
        private Queue<Runnable> queue;
        private bool flag = false;

        public Thread T
        {
            get { return t; }
            set { t = value; }
        }

        private Runnable task;

        public Runnable Task
        {
            get { return task; }
            set { task = value; }
        }

        public MThread(Queue<Runnable> queue)
        {
            this.queue = queue;
        }

        public void Abort()
        {
            flag = false;
            lock (this)
            {
                Monitor.PulseAll(this);
            }

            if (task != null)
            {
                task.Abort();
            }
            //Console.WriteLine(t.ThreadState);
            //if (t.ThreadState == ThreadState.Running)
            //{
            //    t.Abort();
            //}
        }

        public void Execute()
        {
            if (task == null)
            {
                lock (queue)
                {
                    if (queue.Count > 0)
                    {
                        task = queue.Dequeue();
                    }
                }
            }
            
            if(task!=null)
            {
                if (t == null)
                {
                    flag = true;
                    t = new Thread(new ThreadStart(this.ThreadProc));
                    t.Start();
                }
                else
                {
                    if (t.ThreadState == ThreadState.WaitSleepJoin)
                    {
                        lock (this)
                        {
                            Monitor.PulseAll(this);
                        }
                    }
                }
            }
        }

        private void ThreadProc()
        {
            while (flag)
            {
                task.Run();
                task = null;

                if (flag)
                {
                    lock (this)
                    {
                        Monitor.Wait(this);
                    }
                }
            }
        }
    }
}
