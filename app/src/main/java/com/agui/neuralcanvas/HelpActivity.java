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
                "NeuralCanvas 使用说明\n\n" +

                "1. 画布基础操作\n" +
                "• 双击空白处：在点击位置创建节点\n" +
                "• 单指拖拽空白处：移动整张画布\n" +
                "• 双指捏合：自由缩放画布\n" +
                "• 缩放后节点和连线会跟随变化\n\n" +

                "2. 节点操作\n" +
                "• 单击节点：显示内容预览卡片，再点一次收起\n" +
                "• 拖拽节点：移动节点位置\n" +
                "• 长按节点：打开编辑弹窗\n\n" +

                "3. 节点编辑\n" +
                "• 可修改标题、内容、类型、形状\n" +
                "• 类型可用于搜索和分类\n" +
                "• 形状支持：正方形、圆形、椭圆、菱形、三角形、五边形、六边形\n" +
                "• 可从弹窗进入创建连线模式\n" +
                "• 可删除节点\n\n" +

                "4. 连线操作\n" +
                "• 进入连线模式后，点击目标节点建立连线\n" +
                "• 建立连线时可设置文字、颜色、粗细\n" +
                "• 箭头会自动指向目标节点，表示方向关系\n" +
                "• 方向很重要，例如 A 指向 B 和 B 指向 A 含义不同\n\n" +

                "5. 搜索\n" +
                "• 可按关键词和类型搜索节点\n" +
                "• 搜索后自动定位到匹配节点\n" +
                "• 可高亮结果\n\n" +

                "6. 自动保存\n" +
                "• 编辑、拖动、加点、删点、改线后会自动保存\n" +
                "• 切到后台时会再次保存\n" +
                "• 重新打开 app 时自动恢复之前的数据\n\n" +

                "7. AI 助手是什么\n" +
                "• AI 助手可以读取你当前整张图的内容\n" +
                "• 它能看到节点标题、节点内容、节点类型、节点形状\n" +
                "• 它也能看到所有连线关系、箭头方向、连接标签\n" +
                "• 你可以向它提问，也可以让它帮你修改图谱\n\n" +

                "8. 如何使用 AI 助手\n" +
                "• 点击右上角 更多 → AI助手\n" +
                "• 先填写 Base URL、API Key、模型名\n" +
                "• 打开“启用AI”开关\n" +
                "• 点“保存配置”\n" +
                "• 然后在输入框里写你的要求，例如：\n" +
                "  ① 总结当前图谱\n" +
                "  ② 帮我补充3个任务节点\n" +
                "  ③ 把资源节点和目标节点连接起来\n" +
                "  ④ 整理一下当前图谱结构\n\n" +

                "9. 小白如何填写 AI 配置\n" +
                "• Base URL：接口地址，一般是服务商提供的 API 根地址\n" +
                "• 常见格式通常像这样：\n" +
                "  https://你的服务地址/v1\n" +
                "• 不要乱加多余空格\n" +
                "• API Key：服务商给你的密钥，直接完整复制进去\n" +
                "• 模型名：服务商要求你填写的模型名称，例如某个 chat 模型\n" +
                "• 如果你不清楚模型名，就去你所用平台的控制台或文档里看“模型名称”那一栏\n" +
                "• 三项都填对后，再打开“启用AI”\n\n" +

                "10. AI 配置填完后没反应怎么办\n" +
                "• 先检查 Base URL 是否写错\n" +
                "• 再检查 API Key 是否复制完整\n" +
                "• 再检查模型名是否和平台要求一致\n" +
                "• 确认手机能联网\n" +
                "• 某些平台余额不足、权限不足、Key失效，也会请求失败\n" +
                "• 如果返回格式和标准接口不一致，可能需要单独适配\n\n" +

                "11. 知识导入怎么用\n" +
                "• 点击右上角 更多 → 知识导入\n" +
                "• 把一整段文字粘贴进去\n" +
                "• 可以额外补充要求，例如：\n" +
                "  ① 按因果关系建图\n" +
                "  ② 按章节建图\n" +
                "  ③ 只提炼重点\n" +
                "• 点击“开始整理”后，AI 会尝试把文本拆成节点和连接关系\n\n" +

                "12. AI 命令预览\n" +
                "• 当 AI 不只是回答，而是要改你的图时，会先显示命令预览\n" +
                "• 你可以先看 AI 打算创建什么节点、建立什么连线\n" +
                "• 确认没问题，再执行\n" +
                "• 这样可以避免 AI 乱改图谱\n\n" +

                "13. 菜单说明\n" +
                "• 新建节点：手动添加一个节点\n" +
                "• 搜索：按关键词或类型找节点\n" +
                "• AI助手：基于当前图谱问答或改图\n" +
                "• 知识导入：把文本自动整理成节点网络\n" +
                "• 清除全部：删除全部节点和连线\n" +
                "• 帮助：打开本说明页\n\n" +

                "14. 建议使用方式\n" +
                "• 先手动建几个核心节点\n" +
                "• 再用 AI 帮你扩展子节点和连接关系\n" +
                "• 对重要图谱，先看命令预览再执行\n" +
                "• 导入长文本时，尽量分段清晰，AI整理效果会更好"
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
