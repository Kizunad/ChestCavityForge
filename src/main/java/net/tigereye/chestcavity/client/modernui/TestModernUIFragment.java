package net.tigereye.chestcavity.client.modernui;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal Modern UI fragment used to validate the dependency during development.
 */
@OnlyIn(Dist.CLIENT)
public class TestModernUIFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable DataSet savedInstanceState) {
        var context = requireContext();
        var minecraft = Minecraft.getInstance();

        var root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = root.dp(18);
        root.setPadding(padding, padding, padding, padding);

        var background = new ShapeDrawable();
        background.setCornerRadius(root.dp(12));
        background.setColor(0xCC151A1F);
        background.setStroke(root.dp(1), 0xFF4A90E2);
        root.setBackground(background);

        var title = new TextView(context);
        title.setText("ChestCavity Modern UI Demo");
        title.setTextSize(18);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        var description = new TextView(context);
        description.setText("This placeholder view verifies the Modern UI dependency.\nUse the controls below to confirm interactivity.");
        description.setTextSize(14);
        description.setTextColor(0xFFDFDFDF);
        description.setGravity(Gravity.CENTER_HORIZONTAL);
        description.setPadding(0, root.dp(10), 0, root.dp(14));
        root.addView(description, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        var counterLabel = new TextView(context);
        counterLabel.setTextSize(16);
        counterLabel.setTextColor(0xFFE6B422);
        counterLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        counterLabel.setText("Clicks: 0");
        root.addView(counterLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        var clickCounter = new AtomicInteger();
        var incrementButton = new Button(context);
        incrementButton.setText("Increment Counter");
        incrementButton.setOnClickListener(v -> {
            int value = clickCounter.incrementAndGet();
            counterLabel.setText("Clicks: " + value);
        });
        var incrementParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        incrementParams.topMargin = root.dp(14);
        root.addView(incrementButton, incrementParams);

        var closeButton = new Button(context);
        closeButton.setText("Close Screen");
        closeButton.setOnClickListener(v -> minecraft.execute(() -> minecraft.setScreen(null)));
        var closeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        closeParams.topMargin = root.dp(8);
        root.addView(closeButton, closeParams);

        var note = new TextView(context);
        note.setTextSize(12);
        note.setTextColor(0xFF9FA7B3);
        note.setGravity(Gravity.CENTER_HORIZONTAL);
        note.setText("Tip: bind this screen via /testmodernUI while developing layouts.");
        note.setPadding(0, root.dp(12), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Ensure the layout centers within the Modern UI root container.
        var layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.setLayoutParams(layoutParams);

        return root;
    }
}
