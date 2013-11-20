package ws.hoyland.sszs;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;

public class Option extends Dialog implements Observer {

	protected Object result;
	protected Shell shell;
	private Text text;
	private Text text_1;
	private Configuration configuration = Configuration.getInstance();
	private Spinner spinner;
	private Spinner spinner_1;
	private Button btnCheckButton;
	private Button btnCheckButton_1;
	private Spinner spinner_2;
	private Spinner spinner_3;
	private Combo combo;
	private Spinner spinner_4;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public Option(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
		Engine.getInstance().addObserver(this);
	}
	
	private void load(){
		//load & show
		try{
//			if(!flag){
//				InputStream is = Option.class.getResourceAsStream("/qm.ini");
//				this.configuration.load(is);
//				is.close();
//			}			
			if(this.configuration.size()>0){
				spinner.setSelection(Integer.parseInt(this.configuration.getProperty("GROUP_QUANTITY")));
				spinner_1.setSelection(Integer.parseInt(this.configuration.getProperty("TOKEN_QUANTITY")));
				if(Integer.parseInt(this.configuration.getProperty("RECONN_GROUP_QUANTITY_FLAG"))==1){
					btnCheckButton.setSelection(true);
					spinner_2.setEnabled(true);
					spinner_2.setSelection(Integer.parseInt(this.configuration.getProperty("RECONN_GROUP_QUANTITY")));
				}else{
					btnCheckButton.setSelection(false);
					spinner_2.setEnabled(false);
				}
				
				if(Integer.parseInt(this.configuration.getProperty("RECONN_ACCOUNT_QUANTITY_FLAG"))==1){
					btnCheckButton_1.setSelection(true);
					spinner_3.setEnabled(true);
					spinner_3.setSelection(Integer.parseInt(this.configuration.getProperty("RECONN_ACCOUNT_QUANTITY")));
				}else{
					btnCheckButton_1.setSelection(false);
					spinner_3.setEnabled(false);
				}
				
				text.setText(this.configuration.getProperty("ADSL_ACCOUNT"));
				text_1.setText(this.configuration.getProperty("ADSL_PASSWORD"));
				
				spinner_4.setSelection(Integer.parseInt(this.configuration.getProperty("THREAD_COUNT")));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void save(){
		this.configuration.put("GROUP_QUANTITY", spinner.getText());
		this.configuration.put("TOKEN_QUANTITY", spinner_1.getText());
		this.configuration.put("RECONN_GROUP_QUANTITY_FLAG", btnCheckButton.getSelection()?"1":"0");
		this.configuration.put("RECONN_GROUP_QUANTITY", spinner_2.getText());
		this.configuration.put("RECONN_ACCOUNT_QUANTITY_FLAG", btnCheckButton_1.getSelection()?"1":"0");
		this.configuration.put("RECONN_ACCOUNT_QUANTITY", spinner_3.getText());
		this.configuration.put("ADSL_ACCOUNT", text.getText());
		this.configuration.put("ADSL_PASSWORD", text_1.getText());
		this.configuration.put("THREAD_COUNT", spinner_4.getText());
		
		this.configuration.save();
	}


	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		load();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

//	public void close(){
//		this.shell.setVisible(false);
//	}
	
	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Engine.getInstance().deleteObserver(Option.this);
			}
		});
//		shell.addShellListener(new ShellAdapter() {
//			@Override
//			public void shellClosed(ShellEvent e) {
//				shell.setVisible(false);
//				e.doit = false;
//			}
//		});
		
		shell.setSize(426, 280);
		shell.setText("设置");
		
		Rectangle bounds = Display.getDefault().getPrimaryMonitor().getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);
		
		TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setBounds(-1, 2, 424, 215);
		
		TabItem tbtmNewItem = new TabItem(tabFolder, SWT.NONE);
		tbtmNewItem.setText("常规");
		
		Composite composite = new Composite(tabFolder, SWT.NONE);
		tbtmNewItem.setControl(composite);
		
		Label lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setBounds(10, 13, 112, 17);
		lblNewLabel.setText("每个号码发送群数:");
		
		spinner = new Spinner(composite, SWT.BORDER);
		spinner.setMaximum(99999);
		spinner.setMinimum(-1);
		spinner.setSelection(-1);
		spinner.setBounds(128, 10, 69, 20);
		
		Label label = new Label(composite, SWT.NONE);
		label.setText("每个令牌发送群数:");
		label.setBounds(10, 42, 112, 17);
		
		spinner_1 = new Spinner(composite, SWT.BORDER);
		spinner_1.setMaximum(99999);
		spinner_1.setMinimum(-1);
		spinner_1.setSelection(-1);
		spinner_1.setBounds(128, 39, 69, 20);
		
		Label label_2 = new Label(composite, SWT.NONE);
		label_2.setText("线程数量：");
		label_2.setBounds(10, 71, 112, 17);
		
		spinner_4 = new Spinner(composite, SWT.BORDER);
		spinner_4.setMinimum(3);
		spinner_4.setSelection(3);
		spinner_4.setBounds(128, 68, 47, 20);
		
		TabItem tbtmNewItem_1 = new TabItem(tabFolder, SWT.NONE);
		tbtmNewItem_1.setText("高级");
		
		Composite composite_1 = new Composite(tabFolder, SWT.NONE);
		tbtmNewItem_1.setControl(composite_1);
		
		btnCheckButton = new Button(composite_1, SWT.CHECK);
		btnCheckButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(btnCheckButton.getSelection()){
					spinner_2.setEnabled(true);
				}else{
					spinner_2.setEnabled(false);
				}
			}
		});
		btnCheckButton.setBounds(10, 14, 69, 17);
		btnCheckButton.setText("群数重拨");
		
		btnCheckButton_1 = new Button(composite_1, SWT.CHECK);
		btnCheckButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(btnCheckButton_1.getSelection()){
					spinner_3.setEnabled(true);
				}else{
					spinner_3.setEnabled(false);
				}				
			}
		});
		btnCheckButton_1.setBounds(10, 42, 69, 17);
		btnCheckButton_1.setText("帐号重拨");
		
		spinner_2 = new Spinner(composite_1, SWT.BORDER);
		spinner_2.setMaximum(99999);
		spinner_2.setMinimum(1);
		spinner_2.setSelection(10);
		spinner_2.setEnabled(false);
		spinner_2.setBounds(92, 11, 87, 20);
		
		spinner_3 = new Spinner(composite_1, SWT.BORDER);
		spinner_3.setMaximum(99999);
		spinner_3.setMinimum(1);
		spinner_3.setSelection(10);
		spinner_3.setEnabled(false);
		spinner_3.setBounds(92, 39, 87, 20);
		
		Label lblNewLabel_1 = new Label(composite_1, SWT.NONE);
		lblNewLabel_1.setEnabled(false);
		lblNewLabel_1.setBounds(10, 69, 61, 17);
		lblNewLabel_1.setText("宽带连接:");
		
		combo = new Combo(composite_1, SWT.NONE);
		combo.setEnabled(false);
		combo.setBounds(91, 65, 88, 23);
		combo.setText("宽带连接");
		
		Label lblNewLabel_2 = new Label(composite_1, SWT.NONE);
		lblNewLabel_2.setBounds(10, 99, 61, 17);
		lblNewLabel_2.setText("宽带帐号:");
		
		text = new Text(composite_1, SWT.BORDER);
		text.setBounds(92, 96, 139, 20);
		
		Label label_1 = new Label(composite_1, SWT.NONE);
		label_1.setText("宽带密码:");
		label_1.setBounds(10, 125, 61, 17);
		
		text_1 = new Text(composite_1, SWT.BORDER | SWT.PASSWORD);
		text_1.setBounds(92, 122, 139, 20);
		
		Button btnNewButton = new Button(shell, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//save configuration
				save();
				Option.this.shell.dispose();
			}
		});
		btnNewButton.setBounds(236, 221, 80, 27);
		btnNewButton.setText("确定(&O)");
		
		Button btnc = new Button(shell, SWT.NONE);
		btnc.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Option.this.shell.dispose();
			}
		});
		btnc.setText("取消(&C)");
		btnc.setBounds(338, 221, 80, 27);
	}

	@Override
	public void update(Observable obj, Object arg) {
		// TODO Auto-generated method stub
		// 接收来自Engine的消息
	}
}
