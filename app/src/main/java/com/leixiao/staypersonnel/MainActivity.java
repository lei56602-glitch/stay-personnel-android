package com.leixiao.staypersonnel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final int CREATE_XLSX = 4102;
    private static final int CREATE_BACKUP = 4103;
    private static final int OPEN_BACKUP = 4104;
    private static final int INK = Color.rgb(45,55,72);
    private static final int MUTED = Color.rgb(74,85,104);
    private static final int PURPLE = Color.rgb(102,126,234);
    private static final int GREEN = Color.rgb(72,187,120);
    private static final int RED = Color.rgb(229,62,62);
    private static final String[] TYPES = {"宿舍","新房宿舍","自有房","外租房"};
    private static final String[] SEARCH_TYPES = {"综合搜索","按姓名","按电话","按宿舍号","按住宿类型"};
    private JSONArray data = new JSONArray();
    private boolean multiMode = false;
    private String searchType = "all";
    private String searchKeyword = "";
    private String filterType = "";
    private String filterRoom = "";
    private boolean advancedOpen = false;
    private EditText recognizeInput, roomInput, nameInput, phoneInput, searchInput, filterRoomInput;
    private Spinner typeSpinner, filterTypeSpinner;
    private LinearLayout singleArea, multiArea, multiRows, tableRows, statsArea, advancedArea, tabRow;
    private TextView searchLabel, totalCount, totalPeople, matchCount;
    private Button modeButton;
    private final ArrayList<PersonRow> addRows = new ArrayList<>();
    private AlertDialog editDialog;
    private String pendingExportName;
    private boolean compactScreen;
    private boolean bulkMode=false;
    private final java.util.LinkedHashSet<Long> selectedIds=new java.util.LinkedHashSet<>();
    private static final String AUDIT_KEY="stayAuditLog";

    private static class PersonRow {
        EditText name, phone;
        LinearLayout container;
        PersonRow(EditText n, EditText p, LinearLayout c) { name=n; phone=p; container=c; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(224,247,250));
        getWindow().setNavigationBarColor(Color.rgb(243,229,245));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        loadData();
        buildScreen();
        renderTable();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private GradientDrawable gradient(int a, int b, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{a,b});
        d.setCornerRadius(dp(radius)); return d;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    private LinearLayout card(int padding) {
        LinearLayout l = column();
        l.setPadding(dp(padding),dp(padding),dp(padding),dp(padding));
        l.setBackground(shape(Color.argb(205,255,255,255),Color.argb(220,255,255,255),16));
        l.setElevation(dp(5)); return l;
    }
    private void gap(LinearLayout parent, int height) {
        View v = new View(this); parent.addView(v,new LinearLayout.LayoutParams(1,dp(height)));
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private EditText edit(String hint, boolean phone) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setTextSize(14); e.setTextColor(INK);
        e.setHintTextColor(Color.rgb(145,155,170)); e.setHint(hint);
        e.setPadding(dp(12),0,dp(12),0); e.setMinHeight(dp(42));
        e.setBackground(shape(Color.argb(130,255,255,255),Color.argb(180,255,255,255),8));
        if (phone) e.setInputType(InputType.TYPE_CLASS_PHONE);
        else e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return e;
    }
    private Button button(String label, int c1, int c2, int height) {
        Button b = new Button(this); b.setText(label); b.setTextSize(height<=32?12:14);
        b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setPadding(dp(12),0,dp(12),0); b.setMinHeight(dp(height)); b.setMinimumHeight(dp(height));
        b.setBackground(gradient(c1,c2,8)); b.setElevation(dp(2));
        return b;
    }
    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(a);
        s.setBackground(shape(Color.argb(130,255,255,255),Color.argb(180,255,255,255),8));
        return s;
    }
    private void field(LinearLayout parent, String label, View input) {
        LinearLayout wrap=column();
        TextView t=text(label,14,INK,true); wrap.addView(t);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(42)); ip.topMargin=dp(6);
        wrap.addView(input,ip);
        LinearLayout.LayoutParams wp = parent.getOrientation() == LinearLayout.HORIZONTAL
                ? new LinearLayout.LayoutParams(0, -2, 1)
                : new LinearLayout.LayoutParams(-1, -2);
        wp.rightMargin=dp(7);
        parent.addView(wrap,wp);
    }
    private void buildScreen() {
        FrameLayout frame=new FrameLayout(this);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(224,247,250),Color.rgb(232,245,233),Color.rgb(243,229,245)});
        frame.setBackground(bg);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false);
        frame.setFitsSystemWindows(true);
        LinearLayout content=column(); content.setPadding(dp(12),dp(18),dp(12),dp(116));
        scroll.addView(content); frame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header=card(20); header.setGravity(Gravity.CENTER);
        TextView title=text("住宿人员名单统计系统",24,INK,true); title.setGravity(Gravity.CENTER);
        TextView subtitle=text("全离线数据存储 | 智能识别快速录入 | 同宿舍自动合并 | 一键导出Excel",12,MUTED,false);
        subtitle.setGravity(Gravity.CENTER); subtitle.setPadding(0,dp(8),0,0);
        header.addView(title); header.addView(subtitle);
        content.addView(header,new LinearLayout.LayoutParams(-1,-2));
        Button management=button("数据管理与统计",PURPLE,Color.rgb(118,75,162),40);
        LinearLayout.LayoutParams mgp=new LinearLayout.LayoutParams(-1,dp(40));mgp.topMargin=dp(10);content.addView(management,mgp);
        management.setOnClickListener(v->showManagementMenu());
        gap(content,10);

        LinearLayout operation=card(14); content.addView(operation,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout ai=column(); ai.setPadding(dp(12),dp(12),dp(12),dp(12));
        ai.setBackground(shape(Color.argb(22,102,126,234),Color.argb(130,102,126,234),12));
        LinearLayout aiHead=row(); TextView aiTitle=text("📋 智能识别快速录入",15,Color.rgb(76,81,191),true);
        aiHead.addView(aiTitle,new LinearLayout.LayoutParams(0,-2,1));
        Button recognize=button("一键识别填充",PURPLE,Color.rgb(118,75,162),30); aiHead.addView(recognize);
        ai.addView(aiHead);
        TextView tip=text("支持粘贴任意格式的「姓名+电话+宿舍号」文本，自动拆分填充（例：张三 13800001111 101）",12,MUTED,false);
        tip.setPadding(0,dp(8),0,dp(8)); ai.addView(tip);
        recognizeInput=new EditText(this); recognizeInput.setTextSize(14); recognizeInput.setGravity(Gravity.TOP);
        recognizeInput.setHint("请粘贴包含姓名、电话、宿舍号的文本，粘贴后自动识别...");
        recognizeInput.setMinLines(3); recognizeInput.setPadding(dp(12),dp(10),dp(12),dp(10));
        recognizeInput.setBackground(shape(Color.argb(120,255,255,255),Color.argb(180,255,255,255),8));
        ai.addView(recognizeInput,new LinearLayout.LayoutParams(-1,dp(82)));
        recognize.setOnClickListener(v->autoRecognize());
        operation.addView(ai); gap(operation,16);

        boolean compact = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density < 600;
        compactScreen=compact;
        LinearLayout base=row(); typeSpinner=spinner(TYPES); roomInput=edit("请输入宿舍号",false);
        field(base,"住宿类型",typeSpinner); field(base,"宿舍号",roomInput);
        operation.addView(base); gap(operation,14);
        typeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updateRoomVisibility();}
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });

        singleArea=row(); nameInput=edit("请输入姓名",false); phoneInput=edit("请输入联系电话",true);
        field(singleArea,"人员姓名",nameInput); field(singleArea,"联系电话",phoneInput);
        operation.addView(singleArea); gap(operation,12);

        multiArea=column(); multiArea.setPadding(dp(12),dp(12),dp(12),dp(12));
        multiArea.setBackground(shape(Color.argb(100,255,255,255),Color.argb(160,255,255,255),12));
        LinearLayout mh=row(); mh.addView(text("同宿舍人员批量录入（姓名与电话一一对应）",14,INK,true),new LinearLayout.LayoutParams(0,-2,1));
        Button add=button("+ 新增人员",PURPLE,Color.rgb(118,75,162),30); mh.addView(add); multiArea.addView(mh);
        multiRows=column(); LinearLayout.LayoutParams mrp=new LinearLayout.LayoutParams(-1,-2); mrp.topMargin=dp(10); multiArea.addView(multiRows,mrp);
        add.setOnClickListener(v->addPersonRow(multiRows,addRows));
        operation.addView(multiArea); multiArea.setVisibility(View.GONE); gap(operation,12);

        LinearLayout actions=column();
        LinearLayout actionTop=row();
        modeButton=button("切换批量录入模式",Color.rgb(160,174,192),Color.rgb(130,145,165),42);
        Button clear=button("一键清空所有数据",Color.rgb(237,137,54),Color.rgb(221,107,32),42);
        Button export=button("导出Excel",GREEN,Color.rgb(56,161,105),42);
        actionTop.addView(modeButton,new LinearLayout.LayoutParams(0,dp(42),1));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(42),1); ap.leftMargin=dp(8);
        actionTop.addView(clear,ap); actions.addView(actionTop);
        LinearLayout actionBottom=row(); LinearLayout.LayoutParams ebp=new LinearLayout.LayoutParams(-1,dp(42)); ebp.topMargin=dp(14);
        actionBottom.addView(export,new LinearLayout.LayoutParams(-1,dp(42))); actions.addView(actionBottom,ebp);
        operation.addView(actions);
        modeButton.setOnClickListener(v->toggleMode());
        clear.setOnClickListener(v->confirmClear());
        export.setOnClickListener(v->exportExcel());

        gap(operation,18);
        LinearLayout searchCard=column(); searchCard.setPadding(dp(12),dp(12),dp(12),dp(10));
        searchCard.setBackground(shape(Color.argb(100,255,255,255),Color.argb(160,255,255,255),12));
        LinearLayout sh=row(); sh.addView(text("🔍 高级搜索筛选",15,INK,true),new LinearLayout.LayoutParams(0,-2,1));
        Button reset=button("重置搜索",Color.rgb(160,174,192),Color.rgb(130,145,165),30); sh.addView(reset); searchCard.addView(sh);
        reset.setOnClickListener(v->resetSearch());
        tabRow=column(); LinearLayout.LayoutParams tsp=new LinearLayout.LayoutParams(-1,-2);tsp.topMargin=dp(10);searchCard.addView(tabRow,tsp);
        for(int r=0;r<2;r++){LinearLayout line=row();LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(40));if(r>0)lp.topMargin=dp(6);tabRow.addView(line,lp);int start=r==0?0:3,end=r==0?3:5;for(int i=start;i<end;i++){final int idx=i;Button tab=button(SEARCH_TYPES[i],i==0?PURPLE:Color.rgb(180,190,205),i==0?Color.rgb(118,75,162):Color.rgb(160,174,192),34);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(36),1);if(i>start)tp.leftMargin=dp(6);line.addView(tab,tp);tab.setOnClickListener(v->setSearchType(idx));}}
        LinearLayout sr=column();searchLabel=text("关键词",13,INK,true);sr.addView(searchLabel);searchInput=edit("输入关键词搜索（支持模糊匹配）",false);LinearLayout.LayoutParams sip=new LinearLayout.LayoutParams(-1,dp(42));sip.topMargin=dp(6);sr.addView(searchInput,sip);
        LinearLayout searchActions=row();LinearLayout.LayoutParams sap=new LinearLayout.LayoutParams(-1,dp(40));sap.topMargin=dp(8);sr.addView(searchActions,sap);Button search=button("搜索",PURPLE,Color.rgb(118,75,162),38);Button advanced=button("高级筛选",Color.rgb(160,174,192),Color.rgb(130,145,165),38);searchActions.addView(search,new LinearLayout.LayoutParams(0,dp(38),1));LinearLayout.LayoutParams ap2=new LinearLayout.LayoutParams(0,dp(38),1);ap2.leftMargin=dp(8);searchActions.addView(advanced,ap2);searchCard.addView(sr);search.setOnClickListener(v->{searchKeyword=searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);renderTable();});
        searchInput.setOnEditorActionListener((v,id,event)->{searchKeyword=searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);renderTable();return false;});
        advancedArea=column(); advancedArea.setPadding(0,dp(10),0,0); searchCard.addView(advancedArea); advancedArea.setVisibility(View.GONE);
        LinearLayout fr=row(); filterTypeSpinner=spinner(new String[]{"全部类型","宿舍","新房宿舍","自有房","外租房"});
        filterRoomInput=edit("输入宿舍号模糊匹配",false); field(fr,"住宿类型筛选",filterTypeSpinner); field(fr,"宿舍号筛选",filterRoomInput); advancedArea.addView(fr);
        filterTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?>p,View v,int pos,long id){filterType=pos==0?"":TYPES[pos-1];renderTable();}public void onNothingSelected(android.widget.AdapterView<?>p){}});
        filterRoomInput.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){filterRoom=s.toString().trim().toLowerCase(Locale.ROOT);renderTable();}public void afterTextChanged(android.text.Editable e){}});
        advanced.setOnClickListener(v->{advancedOpen=!advancedOpen;advancedArea.setVisibility(advancedOpen?View.VISIBLE:View.GONE);});
        statsArea=row(); statsArea.setPadding(0,dp(12),0,dp(2));
        totalCount=text("总记录数：0 条",13,INK,false); totalPeople=text("总人数：0 人",13,INK,false); matchCount=text("搜索匹配：0 条",13,INK,false);
        statsArea.addView(totalCount,new LinearLayout.LayoutParams(0,-2,1)); statsArea.addView(totalPeople,new LinearLayout.LayoutParams(0,-2,1)); statsArea.addView(matchCount,new LinearLayout.LayoutParams(0,-2,1));
        searchCard.addView(statsArea); operation.addView(searchCard);

        LinearLayout tableCard=card(0); LinearLayout.LayoutParams tcp=new LinearLayout.LayoutParams(-1,-2); tcp.topMargin=dp(14); content.addView(tableCard,tcp);
        HorizontalScrollView tableScroll=new HorizontalScrollView(this); tableScroll.setHorizontalScrollBarEnabled(true);
        LinearLayout table=column();if(!compactScreen)table.setMinimumWidth(dp(790));tableScroll.setFillViewport(true);tableScroll.setHorizontalScrollBarEnabled(!compactScreen);tableScroll.addView(table);tableCard.addView(tableScroll);
        LinearLayout headerRow=row(); headerRow.setPadding(dp(8),dp(12),dp(8),dp(12)); headerRow.setBackgroundColor(Color.argb(185,255,255,255));if(compactScreen)headerRow.setVisibility(View.GONE);
        String[] heads={"序号","住宿类型","宿舍号","人员姓名","联系电话","操作"}; int[] widths={48,95,85,125,130,145};
        for(int i=0;i<heads.length;i++){TextView h=text(heads[i],13,Color.rgb(26,32,44),true); headerRow.addView(h,new LinearLayout.LayoutParams(dp(widths[i]),-2));}
        table.addView(headerRow); tableRows=column(); table.addView(tableRows);
        content.addView(new View(this),new LinearLayout.LayoutParams(1,dp(18)));

        LinearLayout floating=card(8); floating.setOrientation(LinearLayout.HORIZONTAL); floating.setGravity(Gravity.CENTER);
        Button submit=button("添加记录",PURPLE,Color.rgb(118,75,162),48); floating.addView(submit,new LinearLayout.LayoutParams(-1,dp(48)));
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); fp.setMargins(dp(12),0,dp(12),dp(12)); frame.addView(floating,fp);
        submit.setOnClickListener(v->addRecord());
        setContentView(frame);
        updateRoomVisibility();
    }

    private void updateRoomVisibility(){
        if(roomInput==null)return;
        int p=typeSpinner.getSelectedItemPosition();
        boolean show=p==0||p==1;
        roomInput.setVisibility(show?View.VISIBLE:View.GONE);
        if(!show)roomInput.setText("");
    }
    private void toggleMode(){
        multiMode=!multiMode; singleArea.setVisibility(multiMode?View.GONE:View.VISIBLE);
        multiArea.setVisibility(multiMode?View.VISIBLE:View.GONE);
        modeButton.setText(multiMode?"切换单人录入模式":"切换批量录入模式");
        if(multiMode&&addRows.isEmpty())addPersonRow(multiRows,addRows);
        if(!multiMode){multiRows.removeAllViews();addRows.clear();}
        nameInput.setText("");phoneInput.setText("");
    }
    private void addPersonRow(LinearLayout parent,ArrayList<PersonRow> list){
        LinearLayout line=row(); line.setPadding(0,dp(5),0,dp(5));
        LinearLayout fields=row(); EditText n=edit("请输入姓名",false),p=edit("请输入联系电话",true);
        field(fields,"姓名",n); field(fields,"联系电话",p);
        line.addView(fields,new LinearLayout.LayoutParams(0,-2,1));
        Button del=button("删除",RED,Color.rgb(197,48,48),34);LinearLayout.LayoutParams dp1=new LinearLayout.LayoutParams(dp(64),dp(34));dp1.leftMargin=dp(8);line.addView(del,dp1);
        PersonRow pr=new PersonRow(n,p,line);list.add(pr);parent.addView(line);
        del.setOnClickListener(v->{if(list.size()<=1){toast("至少保留1位人员信息");return;}list.remove(pr);parent.removeView(line);});
    }
    private void autoRecognize(){
        String original=recognizeInput.getText().toString().trim();if(original.isEmpty())return;
        String work=original,person="",phone="",room="";
        Matcher pm=Pattern.compile("(?:^|[^\\d])(1[3-9]\\d{9})(?:$|[^\\d])").matcher(work);
        if(pm.find()){phone=pm.group(1);work=pm.replaceFirst(" ");}
        Matcher rm=Pattern.compile("(?:\\d+[栋号楼单元层室房宿舍\\-])*\\d{3,4}[A-Za-z]?").matcher(work);
        if(rm.find()){room=rm.group().replaceAll("[^\\dA-Za-z栋号楼单元层室房宿舍\\-]","").trim();work=work.replace(room," ");}
        else {Matcher pure=Pattern.compile("(?:^|[^\\d])(\\d{3,4}[A-Za-z]?)(?:$|[^\\d])").matcher(work);if(pure.find()){room=pure.group(1);work=pure.replaceFirst(" ");}}
        work=work.replaceAll("姓名|名字|电话|手机|手机号|宿舍|宿舍号|房号|房间号|住宿|类型|：|:|;|；|,|，|、|\\.|。|\\?|？|!|！|\\s","");
        Matcher nm=Pattern.compile("[\\u4e00-\\u9fa5]{2,4}").matcher(work);if(nm.find())person=nm.group();else person=work;
        final String parsedName=person,parsedPhone=phone,parsedRoom=room;
        if(parsedName.isEmpty()&&parsedPhone.isEmpty()&&parsedRoom.isEmpty()){toast("未能识别出有效信息，请检查文本格式");return;}
        String preview="姓名："+(parsedName.isEmpty()?"未识别":parsedName)+"\\n联系电话："+(parsedPhone.isEmpty()?"未识别":parsedPhone)+"\\n宿舍号："+(parsedRoom.isEmpty()?"未识别":parsedRoom);
        new AlertDialog.Builder(this).setTitle("智能识别结果").setMessage(preview+"\\n\\n确认后填入录入表单？").setNegativeButton("重新检查",null).setPositiveButton("确认填充",(d,w)->{if(!parsedName.isEmpty())nameInput.setText(parsedName);if(!parsedPhone.isEmpty())phoneInput.setText(parsedPhone);if(!parsedRoom.isEmpty()){roomInput.setText(parsedRoom);typeSpinner.setSelection(0);updateRoomVisibility();}}).show();
    }
    private JSONArray collectRows(ArrayList<PersonRow> rows){
        JSONArray a=new JSONArray();for(PersonRow r:rows){String n=r.name.getText().toString().trim(),p=r.phone.getText().toString().trim();if(!n.isEmpty()){JSONObject o=new JSONObject();try{o.put("name",n);o.put("phone",p);}catch(Exception ignored){}a.put(o);}}return a;
    }
    private void addRecord(){
        String type=TYPES[typeSpinner.getSelectedItemPosition()],room=roomInput.getText().toString().trim();
        if((type.equals("宿舍")||type.equals("新房宿舍"))&&room.isEmpty()){toast("请输入宿舍号");return;}
        JSONArray people=new JSONArray();
        if(multiMode)people=collectRows(addRows);else{
            String n=nameInput.getText().toString().trim(),p=phoneInput.getText().toString().trim();
            if(n.isEmpty()){toast("请输入人员姓名");return;}JSONObject o=new JSONObject();try{o.put("name",n);o.put("phone",p);}catch(Exception ignored){}people.put(o);
        }
        if(people.length()==0){toast("请至少填写1位人员姓名");return;}
        String duplicates=findDuplicatePeople(people);
        if(!duplicates.isEmpty()){new AlertDialog.Builder(this).setTitle("发现可能重复人员").setMessage(duplicates+"\\n\\n仍然继续录入吗？").setNegativeButton("取消",null).setPositiveButton("继续录入",(d,w)->performAddRecord(type,room,people)).show();return;}
        performAddRecord(type,room,people);
    }
    private String findDuplicatePeople(JSONArray incoming){
        java.util.LinkedHashSet<String> found=new java.util.LinkedHashSet<>();
        for(int i=0;i<incoming.length();i++){JSONObject p=incoming.optJSONObject(i);if(p==null)continue;String n=p.optString("name").trim(),ph=p.optString("phone").replaceAll("\\\\s+","");for(int j=0;j<data.length();j++){JSONObject r=data.optJSONObject(j);JSONArray ps=r==null?null:r.optJSONArray("personList");if(ps==null)continue;for(int k=0;k<ps.length();k++){JSONObject q=ps.optJSONObject(k);if(q==null)continue;String qn=q.optString("name").trim(),qp=q.optString("phone").replaceAll("\\\\s+","");if(n.equals(qn)&&(!ph.isEmpty()?ph.equals(qp):qp.isEmpty())){found.add(n);break;}}}}
        return found.isEmpty()?"":"以下人员可能已经存在：\\n"+android.text.TextUtils.join("、",found);
    }
    private void performAddRecord(String type,String room,JSONArray people){
        if(type.equals("宿舍")){
            JSONObject existing=findDorm(room);
            if(existing!=null){JSONArray old=existing.optJSONArray("personList");int count=0;
                for(int i=0;i<people.length();i++){JSONObject person=people.optJSONObject(i);if(!containsName(old,person.optString("name"))){old.put(person);count++;}}
                toast(count>0?"已自动将"+count+"人合并到【宿舍-"+room+"】":"该宿舍已存在这些人员，无需重复添加");
            }else{data=prepend(record(type,room,people));toast("记录添加成功");}
        }else{data=prepend(record(type,room.isEmpty()?"-":room,people));toast("记录添加成功");}
        nameInput.setText("");phoneInput.setText("");roomInput.setText("");recognizeInput.setText("");
        if(multiMode){multiRows.removeAllViews();addRows.clear();addPersonRow(multiRows,addRows);}
        saveData();renderTable();
    }
    private JSONObject record(String type,String room,JSONArray people){JSONObject o=new JSONObject();try{o.put("id",System.currentTimeMillis());o.put("stayType",type);o.put("roomNum",room);o.put("personList",people);}catch(Exception ignored){}return o;}
    private JSONArray prepend(JSONObject o){JSONArray n=new JSONArray();n.put(o);for(int i=0;i<data.length();i++)n.put(data.optJSONObject(i));return n;}
    private JSONObject findDorm(String room){for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o!=null&&o.optString("stayType").equals("宿舍")&&o.optString("roomNum").equals(room))return o;}return null;}
    private boolean containsName(JSONArray a,String name){if(a==null)return false;for(int i=0;i<a.length();i++)if(a.optJSONObject(i)!=null&&a.optJSONObject(i).optString("name").equals(name))return true;return false;}
    private JSONArray filtered(){
        JSONArray out=new JSONArray();
        for(int i=0;i<data.length();i++){
            JSONObject o=data.optJSONObject(i);if(o==null)continue;
            String type=o.optString("stayType"),room=o.optString("roomNum");
            if(!filterType.isEmpty()&&!type.equals(filterType))continue;
            if(!filterRoom.isEmpty()&&!room.toLowerCase(Locale.ROOT).contains(filterRoom))continue;
            JSONArray persons=o.optJSONArray("personList");boolean match=searchKeyword.isEmpty();
            if(!match){String k=searchKeyword;
                if(searchType.equals("all")){match=room.toLowerCase(Locale.ROOT).contains(k)||type.toLowerCase(Locale.ROOT).contains(k)||personMatches(persons,k,"both");}
                else if(searchType.equals("name"))match=personMatches(persons,k,"name");
                else if(searchType.equals("phone"))match=personMatches(persons,k,"phone");
                else if(searchType.equals("room"))match=room.toLowerCase(Locale.ROOT).contains(k);
                else if(searchType.equals("type"))match=type.toLowerCase(Locale.ROOT).contains(k);
            }
            if(match)out.put(o);
        }return out;
    }
    private boolean personMatches(JSONArray a,String k,String field){if(a==null)return false;for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;if((field.equals("both")||field.equals("name"))&&p.optString("name").toLowerCase(Locale.ROOT).contains(k))return true;if((field.equals("both")||field.equals("phone"))&&p.optString("phone").toLowerCase(Locale.ROOT).contains(k))return true;}return false;}
    private void setSearchType(int index){
        String[] keys={"all","name","phone","room","type"};searchType=keys[index];
        String[] labels={"关键词：","姓名：","电话：","宿舍号：","住宿类型："};
        String[] hints={"输入关键词搜索（支持模糊匹配）","输入姓名模糊搜索","输入电话模糊搜索","输入宿舍号模糊搜索","输入住宿类型模糊搜索"};
        searchLabel.setText(labels[index]);searchInput.setHint(hints[index]);
        for(int r=0;r<tabRow.getChildCount();r++){LinearLayout ln=(LinearLayout)tabRow.getChildAt(r);for(int c=0;c<ln.getChildCount();c++){int i=r==0?c:c+3;Button b=(Button)ln.getChildAt(c);b.setBackground(gradient(i==index?PURPLE:Color.rgb(180,190,205),i==index?Color.rgb(118,75,162):Color.rgb(160,174,192),8));}}
    }
    private void resetSearch(){
        searchKeyword="";searchType="all";filterType="";filterRoom="";searchInput.setText("");filterRoomInput.setText("");filterTypeSpinner.setSelection(0);setSearchType(0);advancedOpen=false;advancedArea.setVisibility(View.GONE);renderTable();
    }
    private void renderTable(){
        if(tableRows==null)return;tableRows.removeAllViews();JSONArray shown=filtered();
        int people=0;for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o!=null&&o.optJSONArray("personList")!=null)people+=o.optJSONArray("personList").length();}
        totalCount.setText("总记录数："+data.length()+" 条");totalPeople.setText("总人数："+people+" 人");matchCount.setText("搜索匹配："+shown.length()+" 条");
        if(shown.length()==0){TextView empty=text("暂无匹配数据",14,MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(36),0,dp(36));tableRows.addView(empty);return;}
        int[] widths={48,95,85,125,130,145};
        for(int i=0;i<shown.length();i++){
            JSONObject o=shown.optJSONObject(i);if(o==null)continue;long id=o.optLong("id");String type=o.optString("stayType"),room=o.optString("roomNum");JSONArray persons=o.optJSONArray("personList");
            LinearLayout line=compactScreen?column():row();line.setPadding(dp(12),dp(12),dp(12),dp(12));line.setBackground(shape(i%2==0?Color.argb(195,255,255,255):Color.argb(155,255,255,255),Color.argb(70,102,126,234),12));if(compactScreen){LinearLayout top=row();top.addView(text("记录 "+(i+1),14,PURPLE,true),new LinearLayout.LayoutParams(0,-2,1));top.addView(text(type+(room.equals("-")?"":" · "+room),14,INK,true));line.addView(top);}
            if(bulkMode){CheckBox choose=new CheckBox(this);choose.setText("选择");choose.setTextSize(12);choose.setChecked(selectedIds.contains(id));choose.setOnCheckedChangeListener((v,checked)->{if(checked)selectedIds.add(id);else selectedIds.remove(id);});line.addView(choose,0);}
            if(!compactScreen){addCell(line,String.valueOf(i+1),widths[0],false);addCell(line,type,widths[1],false);addCell(line,room,widths[2],type.equals("宿舍")&&persons!=null&&persons.length()>=2);}else{TextView rt=text("宿舍号："+(room.equals("-")?"无":room),12,type.equals("宿舍")&&persons!=null&&persons.length()>=2?RED:MUTED,true);rt.setPadding(0,dp(4),0,dp(4));line.addView(rt);}
            StringBuilder names=new StringBuilder(),phones=new StringBuilder();if(persons!=null)for(int j=0;j<persons.length();j++){JSONObject p=persons.optJSONObject(j);if(j>0){names.append("\\n");phones.append("\\n");}names.append(p==null?"":p.optString("name"));phones.append(p==null?"-":p.optString("phone","-").isEmpty()?"-":p.optString("phone"));}
            if(!compactScreen){addCell(line,names.toString(),widths[3],false);addCell(line,phones.toString(),widths[4],false);}else{TextView nt=text("人员："+names.toString().replace("\\n","、"),13,INK,false);line.addView(nt);TextView pt=text("电话："+phones.toString().replace("\\n","、"),13,MUTED,false);line.addView(pt);gap(line,8);}
            LinearLayout ops=row();Button edit=button("修改",PURPLE,Color.rgb(118,75,162),32);Button del=button("删除",RED,Color.rgb(197,48,48),32);
            ops.addView(edit,new LinearLayout.LayoutParams(dp(62),dp(32)));LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(dp(62),dp(32));dlp.leftMargin=dp(5);ops.addView(del,dlp);
            if(compactScreen){LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,-2);op.topMargin=dp(8);line.addView(ops,op);}else line.addView(ops,new LinearLayout.LayoutParams(dp(widths[5]),-2));edit.setOnClickListener(v->openEdit(id));del.setOnClickListener(v->deleteRecord(id));
            line.setOnClickListener(v->openEdit(id));LinearLayout.LayoutParams rowp=new LinearLayout.LayoutParams(-1,-2);if(compactScreen)rowp.bottomMargin=dp(8);tableRows.addView(line,rowp);
        }
    }
    private void addCell(LinearLayout parent,String value,int width,boolean red){TextView t=text(value,13,red?RED:INK,red);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(2),0,dp(2),0);parent.addView(t,new LinearLayout.LayoutParams(dp(width),-2));}

    private void openEdit(long id){
        JSONObject rec=findById(id);if(rec==null)return;
        LinearLayout body=column();body.setPadding(dp(18),dp(12),dp(18),dp(8));
        Spinner st=spinner(TYPES);st.setSelection(typeIndex(rec.optString("stayType")));
        EditText room=edit("请输入宿舍号",false);room.setText(rec.optString("roomNum").equals("-")?"":rec.optString("roomNum"));
        field(body,"住宿类型",st);gap(body,10);field(body,"宿舍号",room);gap(body,10);
        LinearLayout peopleBox=column();ArrayList<PersonRow> rows=new ArrayList<>();
        JSONArray old=rec.optJSONArray("personList");boolean many=rec.optString("stayType").equals("宿舍")&&old!=null&&old.length()>=2;
        final EditText[] singleFields=new EditText[2];
        if(many){
            LinearLayout head=row();head.addView(text("同宿舍人员编辑（姓名与电话一一对应）",14,INK,true),new LinearLayout.LayoutParams(0,-2,1));
            Button add=button("+ 新增人员",PURPLE,Color.rgb(118,75,162),30);head.addView(add);peopleBox.addView(head);
            LinearLayout list=column();peopleBox.addView(list);
            if(old!=null)for(int i=0;i<old.length();i++){JSONObject p=old.optJSONObject(i);addDialogPerson(list,rows,p==null?"":p.optString("name"),p==null?"":p.optString("phone"));}
            add.setOnClickListener(v->addDialogPerson(list,rows,"",""));
        }else{
            JSONObject p=old==null?new JSONObject():old.optJSONObject(0);singleFields[0]=edit("请输入姓名",false);singleFields[1]=edit("请输入联系电话",true);
            if(p!=null){singleFields[0].setText(p.optString("name"));singleFields[1].setText(p.optString("phone"));}
            field(peopleBox,"人员姓名",singleFields[0]);gap(peopleBox,8);field(peopleBox,"联系电话",singleFields[1]);
        }
        body.addView(peopleBox);
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("编辑住宿记录").setView(scroll)
            .setNegativeButton("取消",null).setPositiveButton("保存修改",null).create();
        dlg.setOnShowListener(v->{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
            String type=TYPES[st.getSelectedItemPosition()],rn=room.getText().toString().trim();
            if((type.equals("宿舍")||type.equals("新房宿舍"))&&rn.isEmpty()){toast("请输入宿舍号");return;}
            JSONArray plist=new JSONArray();
            if(many){plist=collectRows(rows);if(plist.length()==0){toast("请至少填写1位人员姓名");return;}}
            else{String n=singleFields[0].getText().toString().trim(),ph=singleFields[1].getText().toString().trim();if(n.isEmpty()){toast("请输入人员姓名");return;}JSONObject p=new JSONObject();try{p.put("name",n);p.put("phone",ph);}catch(Exception ignored){}plist.put(p);}
            removeById(id);
            if(type.equals("宿舍")){JSONObject exist=findDorm(rn);if(exist!=null){JSONArray oldPeople=exist.optJSONArray("personList");for(int i=0;i<plist.length();i++){JSONObject p=plist.optJSONObject(i);if(!containsName(oldPeople,p.optString("name")))oldPeople.put(p);}toast("已保存修改，并自动合并到【宿舍-"+rn+"】");}else{data=prepend(record(type,rn,plist));toast("修改已保存");}}
            else{data=prepend(record(type,rn.isEmpty()?"-":rn,plist));toast("修改已保存");}
            saveData();renderTable();dlg.dismiss();
        });});
        dlg.show();dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);editDialog=dlg;
    }
    private void addDialogPerson(LinearLayout parent,ArrayList<PersonRow> rows,String name,String phone){
        LinearLayout line=row();LinearLayout fields=row();EditText n=edit("请输入姓名",false),p=edit("请输入联系电话",true);n.setText(name);p.setText(phone);
        field(fields,"姓名",n); field(fields,"联系电话",p);
        line.addView(fields,new LinearLayout.LayoutParams(0,-2,1));Button del=button("删除",RED,Color.rgb(197,48,48),32);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(dp(60),dp(32));dp.leftMargin=dp(6);line.addView(del,dp);
        PersonRow pr=new PersonRow(n,p,line);rows.add(pr);parent.addView(line);del.setOnClickListener(v->{if(rows.size()<=1){toast("至少保留1位人员信息");return;}rows.remove(pr);parent.removeView(line);});
    }
    private JSONObject findById(long id){for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o!=null&&o.optLong("id")==id)return o;}return null;}
    private void removeById(long id){JSONArray a=new JSONArray();for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o!=null&&o.optLong("id")!=id)a.put(o);}data=a;}
    private int typeIndex(String type){for(int i=0;i<TYPES.length;i++)if(TYPES[i].equals(type))return i;return 0;}
    private void deleteRecord(long id){new AlertDialog.Builder(this).setMessage("确定要删除这条记录吗？删除后无法恢复！").setNegativeButton("取消",null).setPositiveButton("确定",(d,w)->{removeById(id);saveData();renderTable();}).show();}
    private void confirmClear(){new AlertDialog.Builder(this).setMessage("确定要清空所有数据吗？清空后无法恢复！").setNegativeButton("取消",null).setPositiveButton("确定",(d,w)->{data=new JSONArray();multiMode=false;singleArea.setVisibility(View.VISIBLE);multiArea.setVisibility(View.GONE);modeButton.setText("切换批量录入模式");multiRows.removeAllViews();addRows.clear();saveData();renderTable();}).show();}
    private void loadData(){try{String raw=getSharedPreferences("stay_personnel",MODE_PRIVATE).getString("stayPersonData","[]");data=new JSONArray(raw);}catch(Exception e){data=new JSONArray();}}
    private void saveData(){getSharedPreferences("stay_personnel",MODE_PRIVATE).edit().putString("stayPersonData",data.toString()).apply();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private void showManagementMenu(){
        String[] items={"统计分析","数据备份","恢复备份","批量管理/退出批量管理","删除已选记录","批量修改已选记录","数据修改记录"};
        new AlertDialog.Builder(this).setTitle("数据管理中心").setItems(items,(d,which)->{
            if(which==0)showStatistics();else if(which==1)createBackup();else if(which==2)chooseBackup();else if(which==3){bulkMode=!bulkMode;if(!bulkMode)selectedIds.clear();renderTable();toast(bulkMode?"已进入批量管理，请勾选记录":"已退出批量管理");}else if(which==4)deleteSelected();else if(which==5)editSelected();else showAuditLog();
        }).show();
    }
    private void showStatistics(){
        int total=0;java.util.LinkedHashMap<String,Integer> types=new java.util.LinkedHashMap<>();java.util.LinkedHashMap<String,Integer> rooms=new java.util.LinkedHashMap<>();
        for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o==null)continue;String type=o.optString("stayType"),room=o.optString("roomNum");JSONArray ps=o.optJSONArray("personList");int n=ps==null?0:ps.length();total+=n;types.put(type,types.containsKey(type)?types.get(type)+n:n);if(type.equals("宿舍")||type.equals("新房宿舍"))rooms.put(room,rooms.containsKey(room)?rooms.get(room)+n:n);}
        StringBuilder b=new StringBuilder("住宿总人数：").append(total).append("人\n住宿记录：").append(data.length()).append("条\n宿舍数量：").append(rooms.size()).append("间\n\n各住宿类型人数：\n");
        for(String k:types.keySet())b.append(k).append("：").append(types.get(k)).append("人\n");
        b.append("\n宿舍入住人数排行：\n");ArrayList<String> keys=new ArrayList<>(rooms.keySet());java.util.Collections.sort(keys,(x,y)->rooms.get(y)-rooms.get(x));for(int i=0;i<keys.size()&&i<10;i++)b.append(i+1).append(". ").append(keys.get(i)).append("：").append(rooms.get(keys.get(i))).append("人\n");
        new AlertDialog.Builder(this).setTitle("住宿统计分析").setMessage(b.toString()).setPositiveButton("关闭",null).show();
    }
    private void logAction(String action,String detail){
        try{JSONArray logs=new JSONArray(getSharedPreferences("stay_personnel",MODE_PRIVATE).getString(AUDIT_KEY,"[]"));JSONObject o=new JSONObject();o.put("time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));o.put("action",action);o.put("detail",detail);JSONArray next=new JSONArray();next.put(o);for(int i=0;i<logs.length()&&i<199;i++)next.put(logs.optJSONObject(i));getSharedPreferences("stay_personnel",MODE_PRIVATE).edit().putString(AUDIT_KEY,next.toString()).apply();}catch(Exception ignored){}
    }
    private void showAuditLog(){
        try{JSONArray logs=new JSONArray(getSharedPreferences("stay_personnel",MODE_PRIVATE).getString(AUDIT_KEY,"[]"));StringBuilder b=new StringBuilder();for(int i=0;i<logs.length();i++){JSONObject o=logs.optJSONObject(i);if(o!=null)b.append(o.optString("time")).append("  ").append(o.optString("action")).append("\n").append(o.optString("detail")).append("\n\n");}if(b.length()==0)b.append("暂无修改记录");new AlertDialog.Builder(this).setTitle("数据修改记录（最近200条）").setMessage(b.toString()).setPositiveButton("关闭",null).show();}catch(Exception e){toast("读取修改记录失败");}
    }
    private void createBackup(){pendingExportName="住宿人员数据备份_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.CHINA).format(new Date())+".json";Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,pendingExportName);startActivityForResult(i,CREATE_BACKUP);}
    private void chooseBackup(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,OPEN_BACKUP);}
    private void writeBackup(Uri uri){try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new Exception("无法写入文件");JSONObject root=new JSONObject();root.put("format","stay-personnel-backup");root.put("version",1);root.put("exportedAt",System.currentTimeMillis());root.put("data",data);out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));toast("数据备份已保存");}catch(Exception e){toast("备份失败："+e.getMessage());}}
    private void readBackup(Uri uri){try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in==null)throw new Exception("无法读取文件");byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);JSONObject root=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));JSONArray restored=root.optJSONArray("data");if(restored==null)throw new Exception("备份文件格式不正确");new AlertDialog.Builder(this).setTitle("恢复数据").setMessage("恢复备份会替换当前全部住宿数据，共 "+restored.length()+" 条记录。是否继续？").setNegativeButton("取消",null).setPositiveButton("确认恢复",(d,w)->{data=restored;saveData();logAction("恢复数据备份","恢复记录："+data.length()+"条");renderTable();toast("数据恢复成功");}).show();}catch(Exception e){toast("恢复失败："+e.getMessage());}}
    private void deleteSelected(){if(selectedIds.isEmpty()){toast("请先选择记录");return;}new AlertDialog.Builder(this).setMessage("确定删除选中的 "+selectedIds.size()+" 条记录？").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{JSONArray next=new JSONArray();for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o!=null&&!selectedIds.contains(o.optLong("id")))next.put(o);}int n=selectedIds.size();data=next;selectedIds.clear();saveData();logAction("批量删除","删除记录："+n+"条");renderTable();}).show();}
    private void editSelected(){if(selectedIds.isEmpty()){toast("请先选择记录");return;}LinearLayout box=column();box.setPadding(dp(16),dp(8),dp(16),dp(4));Spinner st=spinner(new String[]{"保持原类型","宿舍","新房宿舍","自有房","外租房"});EditText room=edit("统一宿舍号（宿舍类型必填）",false);field(box,"统一住宿类型",st);gap(box,8);field(box,"统一宿舍号",room);new AlertDialog.Builder(this).setTitle("批量修改").setView(box).setNegativeButton("取消",null).setPositiveButton("应用修改",(d,w)->{String type=st.getSelectedItemPosition()==0?"":TYPES[st.getSelectedItemPosition()-1],rn=room.getText().toString().trim();if((type.equals("宿舍")||type.equals("新房宿舍"))&&rn.isEmpty()){toast("请输入宿舍号");return;}for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o!=null&&selectedIds.contains(o.optLong("id"))){try{if(!type.isEmpty())o.put("stayType",type);if(!rn.isEmpty())o.put("roomNum",rn);else if(!type.isEmpty()&&!type.equals("宿舍")&&!type.equals("新房宿舍"))o.put("roomNum","-");}catch(Exception ignored){}}}selectedIds.clear();saveData();logAction("批量修改","修改选中记录");renderTable();}).show();}
    private void exportExcel(){
        if(data.length()==0){toast("暂无数据，无法导出");return;}
        pendingExportName="住宿人员名单统计_"+new SimpleDateFormat("yyyyMMdd",Locale.CHINA).format(new Date())+".xlsx";
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");intent.putExtra(Intent.EXTRA_TITLE,pendingExportName);
        startActivityForResult(intent,CREATE_XLSX);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent intent){
        super.onActivityResult(requestCode,resultCode,intent);
        if(requestCode==CREATE_BACKUP){if(resultCode==RESULT_OK&&intent!=null&&intent.getData()!=null)writeBackup(intent.getData());return;}
        if(requestCode==OPEN_BACKUP){if(resultCode==RESULT_OK&&intent!=null&&intent.getData()!=null)readBackup(intent.getData());return;}
        if(requestCode!=CREATE_XLSX)return;
        if(resultCode!=RESULT_OK||intent==null||intent.getData()==null){toast("已取消保存");return;}
        try(OutputStream out=getContentResolver().openOutputStream(intent.getData())){if(out==null)throw new Exception("无法打开保存位置");writeWorkbook(out);toast("Excel 文件已保存");}
        catch(Exception e){toast("Excel 保存失败："+e.getMessage());}
    }
    private void writeWorkbook(OutputStream out)throws Exception{
        try(ZipOutputStream zip=new ZipOutputStream(out,StandardCharsets.UTF_8)){
            addZip(zip,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            addZip(zip,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            addZip(zip,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"住宿人员名单\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            addZip(zip,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            StringBuilder sheet=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols><col min=\"1\" max=\"1\" width=\"8\" customWidth=\"1\"/><col min=\"2\" max=\"2\" width=\"14\" customWidth=\"1\"/><col min=\"3\" max=\"3\" width=\"12\" customWidth=\"1\"/><col min=\"4\" max=\"4\" width=\"16\" customWidth=\"1\"/><col min=\"5\" max=\"5\" width=\"20\" customWidth=\"1\"/></cols><sheetData>");
            String[] heads={"序号","住宿类型","宿舍号","人员姓名","联系电话"};int row=1;sheet.append("<row r=\"1\">");for(int c=0;c<heads.length;c++)cell(sheet,c+1,row,heads[c]);sheet.append("</row>");
            int serial=1;for(int i=0;i<data.length();i++){JSONObject rec=data.optJSONObject(i);if(rec==null)continue;JSONArray ps=rec.optJSONArray("personList");if(ps==null)continue;for(int j=0;j<ps.length();j++){JSONObject p=ps.optJSONObject(j);row++;sheet.append("<row r=\"").append(row).append("\">");cell(sheet,1,row,String.valueOf(serial++));cell(sheet,2,row,rec.optString("stayType"));cell(sheet,3,row,rec.optString("roomNum"));cell(sheet,4,row,p==null?"-":p.optString("name","-"));cell(sheet,5,row,p==null?"-":p.optString("phone","-"));sheet.append("</row>");}}
            sheet.append("</sheetData></worksheet>");addZip(zip,"xl/worksheets/sheet1.xml",sheet.toString());zip.finish();
        }
    }
    private void cell(StringBuilder b,int col,int row,String value){b.append("<c r=\"").append(colName(col)).append(row).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(value)).append("</t></is></c>");}
    private String colName(int n){StringBuilder s=new StringBuilder();while(n>0){int r=(n-1)%26;s.insert(0,(char)('A'+r));n=(n-1)/26;}return s.toString();}
    private String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    private void addZip(ZipOutputStream z,String name,String value)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(value.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
}
