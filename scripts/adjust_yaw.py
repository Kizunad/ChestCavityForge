import json
import sys

def adjust_yaw(filename, output, offset):
    with open(filename, "r", encoding="utf-8") as f:
        data = json.load(f)

    # 遍历 animations → bones → 每个 bone → rotation
    for anim_name, anim in data.get("animations", {}).items():
        bones = anim.get("bones", {})
        for bone_name, bone in bones.items():
            rotation = bone.get("rotation", {})
            for t, keyframe in rotation.items():
                if "vector" in keyframe and len(keyframe["vector"]) >= 2:
                    keyframe["vector"][1] += offset   # y 分量加偏移角度

    with open(output, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print(f"✅ 已保存到 {output}，yaw 偏移 {offset}°")

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("用法: python adjust_yaw.py 输入文件 输出文件 偏移角度")
        print("例子: python adjust_yaw.py anim.json anim_out.json 90")
    else:
        adjust_yaw(sys.argv[1], sys.argv[2], float(sys.argv[3]))
