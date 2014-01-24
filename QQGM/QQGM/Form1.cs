﻿using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using ws.hoyland;
using System.Threading;
using System.IO;
using System.Text.RegularExpressions;
using System.Net;
using System.Configuration;

namespace QQGM
{
    public partial class Form1 : Form
    {
        private delegate void Delegate();
        private Delegate dlg;

        private bool isLogin = false;
        private bool running = false;
        private DataTable table = new DataTable();
        private int type = 0;
        private Configuration cfa = null;
        //private bool ns = false;

        public Form1()
        {
            InitializeComponent();

            table.Columns.Add("ID", Type.GetType("System.String"));
            table.Columns.Add("帐号", Type.GetType("System.String"));
            table.Columns.Add("密码", Type.GetType("System.String"));
            table.Columns.Add("状态", Type.GetType("System.String"));
            table.Columns.Add("问题1", Type.GetType("System.String"));
            table.Columns.Add("答案1", Type.GetType("System.String"));
            table.Columns.Add("问题2", Type.GetType("System.String"));
            table.Columns.Add("答案2", Type.GetType("System.String"));
            table.Columns.Add("问题3", Type.GetType("System.String"));
            table.Columns.Add("答案3", Type.GetType("System.String"));

            dataGridView1.DataSource = table;
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            cfa = ConfigurationManager.OpenExeConfiguration(ConfigurationUserLevel.None);

            if (cfa == null)
            {
                MessageBox.Show("加载配置文件失败!");
            }

            comboBox1.SelectedIndex = 0;
            ConfigurationManager.RefreshSection("appSettings");
            try
            {
                comboBox2.SelectedIndex = Int32.Parse(cfa.AppSettings.Settings["CPT_TYPE"].Value);
                textBox2.Text = cfa.AppSettings.Settings["ACCOUNT"].Value;
                if ("True".Equals(cfa.AppSettings.Settings["REM_PASSWORD"].Value))
                {
                    checkBox1.Checked = true;
                    textBox3.Text = cfa.AppSettings.Settings["PASSWORD"].Value;
                }

                if ("True".Equals(cfa.AppSettings.Settings["AUTO_LOGIN"].Value))
                {
                    checkBox2.Checked = true;
                    //TODO, autologin
                }

                setlogin();

                if (checkBox2.Checked)
                {//自动登录 
                    //Thread lt;
                    //lt = new Thread(login);
                    //lt.Start();

                    System.Timers.Timer t = new System.Timers.Timer(1000);
                    //实例化Timer类，设置间隔时间为10000毫秒；   
                    t.Elapsed +=
                    new System.Timers.ElapsedEventHandler(login);
                    //到达时间的时候执行事件；   
                    t.AutoReset = false;
                    //设置是执行一次（false）还是一直执行(true)；   
                    t.Enabled = true;
                    //是否执行System.Timers.Timer.Elapsed事件；  
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }

            //comboBox2.SelectedIndex = 0;
        }

        private void 退出XToolStripMenuItem_Click(object sender, EventArgs e)
        {
            Application.Exit();
        }

        private void 关于AToolStripMenuItem_Click(object sender, EventArgs e)
        {
            AboutBox1 about = new AboutBox1();
            about.ShowDialog();
        }

        private void button1_Click(object sender, EventArgs e)
        {
            if (comboBox2.Enabled == true)
            {
                //button1.Enabled = false;
                System.Timers.Timer t = new System.Timers.Timer(1);
                //实例化Timer类，设置间隔时间为10000毫秒；   
                t.Elapsed +=
                new System.Timers.ElapsedEventHandler(login);
                //到达时间的时候执行事件；   
                t.AutoReset = false;
                //设置是执行一次（false）还是一直执行(true)；   
                t.Enabled = true;
                /**
                Thread lt;
                lt = new Thread(login, null, null);
                lt.Start();**/
            }
            else
            {
                label10.Text = "密码:";
                button1.Text = "登录";
                comboBox2.Enabled = true;
                checkBox1.Visible = true;
                checkBox2.Visible = true;

                textBox2.Visible = true;
                textBox3.Visible = true;

                label15.Text = "";
                label15.Visible = false;

                label16.Text = "";
                label16.Visible = false;

            }
        }

        public void login(object source, System.Timers.ElapsedEventArgs e)
        {
            dlg = delegate()
            {
                button1.Enabled = false;
                int nAppId;         // 软件ＩＤ，开发者分成必要参数。登录开发者后台【我的软件】获得！
                string lpAppKey;    // 软件密钥，开发者分成必要参数。登录开发者后台【我的软件】获得！



                //login
                toolStripStatusLabel1.Text = "正在登录...";

                string username, password;
                int ret = -1;
                int score = 0;
                username = textBox2.Text;
                password = textBox3.Text;


                if (comboBox2.SelectedIndex == 0)
                {
                    nAppId = 175;
                    lpAppKey = "8c31eb9cf312478eda8301b87232e731";

                    YDMWrapper.YDM_SetAppInfo(nAppId, lpAppKey);
                    ret = YDMWrapper.YDM_Login(username, password);
                }
                else if (comboBox2.SelectedIndex == 1)
                {
                    nAppId = 95099;
                    lpAppKey = "08573f0698dc43b4b35761c9ab64f014";
                    ret = UUWrapper.uu_login(username, password);
                }

                if (ret > 0)
                {
                    isLogin = true;
                    toolStripStatusLabel1.Text = "登录成功，ID=" + ret.ToString();

                    if (comboBox2.SelectedIndex == 0)
                    {
                        //获取题分
                        score = YDMWrapper.YDM_GetBalance(username, password);
                    }
                    else
                    {
                        score = UUWrapper.uu_getScore(username, password);
                    }
                    //toolStripStatusLabel1.Text = "1";
                    cfa = ConfigurationManager.OpenExeConfiguration(ConfigurationUserLevel.None);

                    if (cfa == null)
                    {
                        MessageBox.Show("加载配置文件失败!");
                    }

                    ConfigurationManager.RefreshSection("appSettings");
                    //toolStripStatusLabel1.Text = "2:" + cfa.AppSettings + "/" + cfa.AppSettings.Settings["ACCOUNT"] + "/" + textBox2;
                    //保存登录参数
                    cfa.AppSettings.Settings["ACCOUNT"].Value = textBox2.Text;
                    //toolStripStatusLabel1.Text = "2.1";
                    if (checkBox1.Checked)
                    {
                        //toolStripStatusLabel1.Text = "2.2";
                        cfa.AppSettings.Settings["PASSWORD"].Value = textBox3.Text;
                        //toolStripStatusLabel1.Text = "2.3";
                    }
                    else
                    {
                        //toolStripStatusLabel1.Text = "2.4";
                        cfa.AppSettings.Settings["PASSWORD"].Value = "";
                        //toolStripStatusLabel1.Text = "2.5";
                    }
                    //toolStripStatusLabel1.Text = "2.6";
                    cfa.AppSettings.Settings["REM_PASSWORD"].Value = checkBox1.Checked.ToString();
                    //toolStripStatusLabel1.Text = "2.7";
                    cfa.AppSettings.Settings["AUTO_LOGIN"].Value = checkBox2.Checked.ToString();
                    //toolStripStatusLabel1.Text = "3";
                    cfa.Save();
                    //toolStripStatusLabel1.Text = "4";

                    label10.Text = "题分:";
                    button1.Text = "切换帐号";
                    comboBox2.Enabled = false;
                    checkBox1.Visible = false;
                    checkBox2.Visible = false;

                    textBox2.Visible = false;
                    textBox3.Visible = false;

                    label15.Text = textBox2.Text;
                    label15.Visible = true;

                    label16.Text = score.ToString();
                    label16.Visible = true;
                    //toolStripStatusLabel1.Text = "5";
                }
                else
                {
                    isLogin = false;
                    toolStripStatusLabel1.Text = "登陆失败，错误代码：" + ret.ToString();
                }

                button1.Enabled = true;
                //toolStripStatusLabel1.Text = "6";
                ready();
            };
            //toolStripStatusLabel1.Text = "7";
            this.BeginInvoke(dlg);
            //toolStripStatusLabel1.Text = "8";
        }

        private void ready()
        {
            if (isLogin && table.Rows.Count > 0&&comboBox1.SelectedIndex!=0)
            {
                button2.Enabled = true;
            }
            else
            {
                button2.Enabled = false;
            }
        }

        private void 导入帐号LToolStripMenuItem_Click(object sender, EventArgs e)
        {
            dlg = delegate()
            {
                OpenFileDialog dialog = new OpenFileDialog();
                dialog.Filter = "所有文件(*.*)|*.*";

                if (dialog.ShowDialog() == DialogResult.OK)
                {
                    table.Clear();

                    string fn = dialog.FileName;
                    FileStream fs = new FileStream(fn, FileMode.Open);
                    StreamReader m_streamReader = new StreamReader(fs);
                    m_streamReader.BaseStream.Seek(0, SeekOrigin.Begin);
                    int i = 0;
                    string line = null;
                    while ((line = m_streamReader.ReadLine()) != null)
                    {
                        if (!line.Equals(""))
                        {
                            line = (++i) + "----" + line;
                            string[] lns = Regex.Split(line, "----");
                            List<string> listArr = new List<string>();
                            listArr.AddRange(lns);
                            listArr.Insert(3, "初始化");
                            lns = listArr.ToArray();

                            DataRow row = table.NewRow();
                            //row[0] = ++i;
                            for (int m = 0; m < table.Columns.Count; m++)
                            {
                                row[m] = lns[m];
                            }
                            //row[1] = lns[0];
                            table.Rows.Add(row);

                            dataGridView1.DataSource = table;
                        }

                    }
                    m_streamReader.Close();
                    m_streamReader.Dispose();
                    fs.Close();
                    fs.Dispose();

                    ready();
                }
            };
            this.BeginInvoke(dlg);
        }

        private void button2_Click(object sender, EventArgs e)
        {
            if (!running)
            {
                type = comboBox1.SelectedIndex;
                if (type == 0)
                {
                    MessageBox.Show("请选择操作类型");
                    return;
                }
                ConfigurationManager.RefreshSection("appSettings");
                int mt = Int32.Parse(cfa.AppSettings.Settings["THREAD_COUNT"].Value);
                Console.WriteLine("MT:" + mt);
                ThreadPool.SetMinThreads(1, 0);
                ThreadPool.SetMaxThreads(mt, 0);
                Task task = null;
                for (int i = 0; i < table.Rows.Count; i++)
                {
                    task = new Task();
                    task.ID = Int32.Parse((string)table.Rows[i]["ID"]);
                    task.Account = (string)table.Rows[i][1];
                    task.Password = (string)table.Rows[i][2];

                    if (table.Rows[i][4] != null && !table.Rows[i][4].Equals(""))
                    {
                        task.Isdna = true;
                        task.Q1 = (string)table.Rows[i][4];
                        task.A1 = (string)table.Rows[i][5];
                        task.Q2 = (string)table.Rows[i][6];
                        task.A2 = (string)table.Rows[i][7];
                        task.Q3 = (string)table.Rows[i][8];
                        task.A3 = (string)table.Rows[i][9];
                    }
                    else
                    {
                        task.Isdna = false;
                    }
                    ThreadPool.QueueUserWorkItem(new WaitCallback(process), task);
                }
                button2.Text = "结束";
            }
            else
            {
                button2.Text = "开始";
                //ThreadPool.
                //stop
            }
            running = !running;
        }

        private void process(Object stateInfo)
        {
            Task task = (Task)stateInfo;
            task.process(this, this.type);
        }

        private void textBox3_KeyUp(object sender, KeyEventArgs e)
        {
            if (e.KeyValue == 13)
            {
                button1.Focus();
                button1.PerformClick();
            }
            //Console.WriteLine(e.KeyValue);
        }

        private void 选项OToolStripMenuItem_Click(object sender, EventArgs e)
        {
            new Option().ShowDialog();
        }

        private void checkBox2_CheckedChanged(object sender, EventArgs e)
        {
            if (checkBox2.Checked)
            {
                checkBox1.Checked = true;
            }
        }

        private void checkBox1_CheckedChanged(object sender, EventArgs e)
        {
            if (!checkBox1.Checked)
            {
                checkBox2.Checked = false;
            }
        }

        private void textBox3_Enter(object sender, EventArgs e)
        {
            textBox3.SelectAll();
            //           ns = true;
        }

        /**
        private void textBox3_MouseDown(object sender, MouseEventArgs e)
        {
            if (ns)
            {
                textBox3.SelectAll();
                ns = false;
            }
        }**/

        private void comboBox2_SelectedIndexChanged(object sender, EventArgs e)
        {
            setlogin();
        }

        private void setlogin()
        {
            if (comboBox2.SelectedIndex == 2)
            {
                textBox2.Enabled = false;
                textBox3.Enabled = false;
                checkBox1.Enabled = false;
                checkBox2.Enabled = false;
                button1.Enabled = false;
            }
            else
            {
                textBox2.Enabled = true;
                textBox3.Enabled = true;
                checkBox1.Enabled = true;
                checkBox2.Enabled = true;
                button1.Enabled = true;
            }
        }

        public void setImage(byte[] bs, int size)
        {
            dlg = delegate()
            {
                MemoryStream ms = new MemoryStream();
                ms.Write(bs, 0, size);
                pictureBox1.Image = Image.FromStream(ms);
                ms.Close();
            };

            this.BeginInvoke(dlg);
        }

        public void info(int tid, string info)
        {
            dlg = delegate()
            {
                this.table.Rows[tid - 1][3] = info;
            };
            this.BeginInvoke(dlg);
        }

        private void comboBox1_SelectedIndexChanged(object sender, EventArgs e)
        {
            ready();
        }
    }
}