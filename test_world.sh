#!/usr/bin/env bash
# BlackBoxPro 世界管理 Action 集成测试
# 前提：MC 客户端已启动 neoforge-1.21.1 Mod，HTTP server enabled，处于主菜单
# 用法：bash test_world.sh

BASE="http://127.0.0.1:25580/api/command"
PASS=0
FAIL=0
WORLD_NAME="bbpro_test_$(date +%s)"

post() {
  curl -s -X POST "$BASE$2" -H "Content-Type: application/json" -d "$1"
}

run_test() {
  local name="$1" body="$2" expect_status="$3" timeout_param="$4"
  printf "%-45s" "  $name"
  local resp
  resp=$(post "$body" "$timeout_param")
  local status
  status=$(echo "$resp" | python -c "import sys,json; print(json.load(sys.stdin).get('status','?'))" 2>/dev/null)
  if [ "$status" = "$expect_status" ]; then
    echo "PASS  ($status)"
    PASS=$((PASS+1))
  else
    echo "FAIL  (expected=$expect_status got=$status)"
    echo "       response: $resp"
    FAIL=$((FAIL+1))
  fi
}

echo "=== BlackBoxPro World Management Integration Test ==="
echo "HTTP endpoint: $BASE"
echo "Test world: $WORLD_NAME"
echo ""

# 1. 连通性检查
echo "[Phase 1] 连通性"
run_test "query_player_state (主菜单应失败)" \
  '{"action":"query_player_state"}' \
  "failure"

# 2. 创建世界
echo ""
echo "[Phase 2] 创建世界"
run_test "create_world" \
  "{\"action\":\"create_world\",\"params\":{\"name\":\"$WORLD_NAME\"}}" \
  "success" "?timeout=120"

# 3. 验证 create_world 成功后已直接进入世界
echo ""
echo "[Phase 3] 验证 create_world 的原子完成语义"
run_test "query_player_state (create_world 后应在世界内)" \
  '{"action":"query_player_state"}' \
  "success"

run_test "query_world_state" \
  '{"action":"query_world_state"}' \
  "success"

run_test "swing_arm" \
  '{"action":"swing_arm","params":{"hand":"main_hand"}}' \
  "success"

run_test "player_look" \
  '{"action":"player_look","params":{"yaw":90.0,"pitch":-30.0,"onGround":true}}' \
  "success"

# 4. 离开世界
echo ""
echo "[Phase 4] 离开世界"
run_test "leave_world" \
  '{"action":"leave_world"}' \
  "success"

# 5. 验证 leave_world 成功后已直接回到主菜单
echo ""
echo "[Phase 5] 验证 leave_world 的原子完成语义"
run_test "query_player_state (leave_world 后主菜单应失败)" \
  '{"action":"query_player_state"}' \
  "failure"

# 6. 重新加入
echo ""
echo "[Phase 6] 重新加入世界"
run_test "join_world" \
  "{\"action\":\"join_world\",\"params\":{\"levelName\":\"$WORLD_NAME\"}}" \
  "success" "?timeout=120"

# 7. 验证 join_world 成功后已直接进入世界
echo ""
echo "[Phase 7] 验证 join_world 的原子完成语义"
run_test "query_player_state (join_world 后应在世界内)" \
  '{"action":"query_player_state"}' \
  "success"

# 8. 最终离开
echo ""
echo "[Phase 8] 最终离开"
run_test "leave_world (最终)" \
  '{"action":"leave_world"}' \
  "success"

# 9. 验证最终 leave_world 成功后已回到主菜单
echo ""
echo "[Phase 9] 验证最终 leave_world 的原子完成语义"
run_test "query_player_state (最终 leave_world 后主菜单应失败)" \
  '{"action":"query_player_state"}' \
  "failure"

# 10. 边界测试
echo ""
echo "[Phase 10] 边界测试"
run_test "leave_world (不在世界中)" \
  '{"action":"leave_world"}' \
  "failure"

run_test "join_world (不存在的世界)" \
  '{"action":"join_world","params":{"levelName":"nonexistent_world_99999"}}' \
  "failure" "?timeout=30"

run_test "create_world (缺少 name 参数)" \
  '{"action":"create_world","params":{}}' \
  "failure"

run_test "unknown_action" \
  '{"action":"this_action_does_not_exist"}' \
  "failure"

# 结果汇总
echo ""
echo "=== 结果: $PASS passed, $FAIL failed ==="
[ $FAIL -eq 0 ] && exit 0 || exit 1
