package com.agui.neuralcanvas;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Toolbar toolbar = findViewById(R.id.helpToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("帮助");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView content = findViewById(R.id.helpContent);
        content.setText(
                "NeuralCanvas 功能说明\n\n" +
                "1. 画布操作\n" +
                "• 双击空白处：在点击位置创建节点\n" +
                "• 单指拖拽空白处：移动整张画布\n" +
                "• 双指捏合：自由缩放画布\n\n" +
                "2. 节点操作\n" +
                "• 单击节点：显示内容预览卡片，再点一次收起\n" +
                "• 拖拽节点：移动节点位置\n" +
                "• 长按节点：打开编辑弹窗\n\n" +
                "3. 节点编辑\n" +
                "• 可修改标题、内容、类型、形状\n" +
                "• 节点形状支持：正方形、圆形、椭圆、菱形、三角形、五边形、六边形\n" +
                "• 可从弹窗进入创建连线模式\n" +
                "• 可删除节点\n\n" +
                "4. 连线操作\n" +
                "• 进入连线模式后，点击目标节点建立连线\n" +
                "• 建立连线时可设置文字、颜色、粗细\n" +
                "• 箭头会自动指向目标节点边缘\n" +
                "• 粗线和细线会随缩放尽量保留差异\n\n" +
                "5. 搜索\n" +
                "• 可按关键词和类型搜索节点\n" +
                "• 搜索后自动定位到第一个匹配节点\n" +
                "• 可高亮结果\n\n" +
                "6. 保存\n" +
                "• 编辑、拖动、加点、删点、改线后自动保存\n" +
                "• 退出后台时再次保存\n" +
                "• 重新打开 app 自动恢复数据\n\n" +
                "7. 菜单\n" +
                "• 新建节点：添加节点\n" +
                "• 搜索：搜索节点\n" +
                "• 清除全部：删除全部节点和连线\n" +
                "• 帮助：打开本页面"
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
