<template>
  <div class="admin-wrap">
    <div class="admin-shell">
      <aside class="admin-nav">
        <div class="admin-nav__brand">
          <svg class="admin-nav__mark" width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.4" />
            <path d="M12 2c4 3.5 4 16.5 0 20" stroke="currentColor" stroke-width="1.1" opacity=".55"/>
            <path d="M2 12c3.5-4 16.5-4 20 0" stroke="currentColor" stroke-width="1.1" opacity=".55"/>
            <circle cx="12" cy="12" r="2.4" fill="currentColor"/>
          </svg>
          <div class="admin-nav__name">
            <strong>GIS 管理控制台</strong>
            <span class="mono">运营 · 监控</span>
          </div>
        </div>
        <nav class="admin-nav__groups">
          <div v-for="g in navGroups" :key="g.name" class="admin-nav__group">
            <div class="admin-nav__groupname">{{ g.name }}</div>
            <button
              v-for="t in g.items"
              :key="t.key"
              class="admin-navitem"
              :class="{ active: tab === t.key }"
              :title="t.label"
              @click="tab = t.key"
            >
              <svg class="admin-navitem__icon" width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <g :transform="'scale(0.72) translate(4.8 4.8)'">
                  <template v-for="ic in navIcons[t.key]" :key="ic.d">
                    <path v-if="ic.d" :d="ic.d" stroke="currentColor" :stroke-width="ic.w || 1.8" stroke-linecap="round" stroke-linejoin="round"/>
                    <circle v-else-if="ic.c" :cx="ic.c[0]" :cy="ic.c[1]" :r="ic.r || 3.4" :stroke="ic.fill ? 'none' : 'currentColor'" :fill="ic.fill ? 'currentColor' : 'none'" :stroke-width="ic.w || 1.8"/>
                  </template>
                </g>
              </svg>
              <span class="admin-navitem__label">{{ t.label }}</span>
            </button>
          </div>
        </nav>
        <div class="admin-nav__foot">
          <button class="admin-logout" @click="logout">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M14 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-2m4-6h4m0 0l-3-3m3 3l-3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
            退出登录
          </button>
        </div>
      </aside>
      <div class="admin-main">
        <header class="admin-pagehead">
          <div>
            <h2 class="admin-pagehead__title">{{ tabs.find((t) => t.key === tab)?.label ?? '' }}</h2>
            <p class="admin-pagehead__desc">{{ activeDesc }}</p>
          </div>
          <div class="admin-pagehead__meta mono">GIS 物流配送 · 运营监控</div>
        </header>

    <!-- 订单统计 -->
    <section ref="sectionEl" class="admin-section" v-if="tab === 'stats'">
      <div class="admin-kpis kpi-row">
        <div class="admin-kpi glass accent">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">TOTAL</span></div>
          <div class="admin-kpi__num mono">{{ totalOrders }}<span class="admin-kpi__unit">单</span></div>
          <div class="admin-kpi__label">月度订单总量</div>
        </div>
        <div class="admin-kpi glass">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">GMV</span></div>
          <div class="admin-kpi__num mono">¥{{ fmtMoney(totalFen) }}</div>
          <div class="admin-kpi__label">月度成交总额</div>
        </div>
        <div class="admin-kpi glass">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">RANGE</span></div>
          <div class="admin-kpi__num mono">{{ stats?.monthly.length ?? 0 }}<span class="admin-kpi__unit">月</span></div>
          <div class="admin-kpi__label">覆盖月份</div>
        </div>
      </div>
      <div class="admin-chartrow">
        <div class="admin-tableglass glass admin-chartglass">
          <div class="admin-tablehead">
            <span class="admin-tabletitle">月度订单量与金额</span>
            <el-button size="small" @click="loadStats">刷新</el-button>
          </div>
          <div ref="monthlyChartEl" class="admin-chartbox"></div>
        </div>
        <div class="admin-tableglass glass admin-chartglass admin-chartglass--slim">
          <div class="admin-tablehead">
            <span class="admin-tabletitle">订单区域分布</span>
          </div>
          <div ref="regionChartEl" class="admin-chartbox"></div>
        </div>
      </div>
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">热门配送路线 Top 10</span>
        </div>
        <div ref="topRoutesChartEl" class="admin-chartbox admin-chartbox--tall"></div>
      </div>
    </section>

    <!-- 个人中心 -->
    <section class="admin-section" v-if="tab === 'profile'">
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">个人中心</span>
          <el-button size="small" @click="loadProfile">刷新</el-button>
        </div>
        <div v-if="profile" class="admin-profile">
          <div class="admin-profile__row">
            <div class="admin-profile__item">
              <div class="admin-profile__label">账号 ID</div>
              <div class="admin-profile__val mono">{{ profile.id }}</div>
            </div>
            <div class="admin-profile__item">
              <div class="admin-profile__label">角色</div>
              <div class="admin-profile__val mono">{{ profile.role }}</div>
            </div>
            <div class="admin-profile__item">
              <div class="admin-profile__label">手机号（脱敏）</div>
              <div class="admin-profile__val mono">{{ profile.phoneMasked }}</div>
            </div>
            <div class="admin-profile__item">
              <div class="admin-profile__label">姓名</div>
              <div class="admin-profile__val mono">{{ profile.name }}</div>
            </div>
          </div>
          <div class="admin-profile__group">
            <div class="admin-profile__subhead">修改姓名</div>
            <div class="admin-profile__inline">
              <el-input v-model="editName" size="small" placeholder="姓名" style="width:200px" />
              <el-button size="small" type="primary" @click="saveProfileName">保存</el-button>
            </div>
          </div>
          <div class="admin-profile__group">
            <div class="admin-profile__subhead">修改密码</div>
            <div class="admin-profile__inline">
              <el-input v-model="pwdForm.oldPassword" size="small" placeholder="原密码" type="password" style="width:200px" />
              <el-input v-model="pwdForm.newPassword" size="small" placeholder="新密码（≥6位）" type="password" style="width:200px" />
              <el-button size="small" type="primary" @click="savePassword">更新</el-button>
            </div>
          </div>
        </div>
        <div v-else class="admin-empty">加载中…</div>
      </div>
    </section>

    <!-- 订单管理 -->
    <section class="admin-section" v-if="tab === 'orders'">
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">订单管理</span>
          <el-select v-model="orderFilter.status" placeholder="全部状态" clearable size="small" style="width:120px" @change="loadOrderMgmt">
            <el-option v-for="st in orderStatusOptions" :key="st.value" :label="st.label" :value="st.value" />
          </el-select>
          <el-select v-model="orderFilter.staffId" placeholder="全部员工" clearable filterable size="small" style="width:150px" @change="loadOrderMgmt">
            <el-option v-for="st in staffList" :key="st.id" :label="'员工 ' + st.id + ' · ' + st.licenseNo" :value="st.id" />
          </el-select>
          <el-select v-model="orderFilter.userId" placeholder="全部用户" clearable filterable size="small" style="width:140px" @change="loadOrderMgmt">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.label" :value="u.id" />
          </el-select>
          <el-date-picker v-model="orderFilter.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" size="small" style="width:260px" @change="loadOrderMgmt" />
          <el-button size="small" @click="loadOrderMgmt">刷新</el-button>
        </div>
        <el-table :data="orderMgmt.content" class="admin-table">
          <el-table-column prop="id" label="ID" width="70" class-name="mono" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ orderStatusOptions.find((o) => o.value === row.status)?.label ?? row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="userId" label="用户" width="80" class-name="mono" />
          <el-table-column prop="staffId" label="员工" width="80" class-name="mono" />
          <el-table-column label="金额" width="100" class-name="mono">
            <template #default="{ row }">¥{{ fmtMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="fromRegion" label="出发" width="90" />
          <el-table-column prop="toRegion" label="到达" width="90" />
          <el-table-column prop="createdAt" label="创建时间" class-name="mono" />
        </el-table>
        <div class="admin-pager mono">共 {{ orderMgmt.totalElements }} 条 · 第 {{ orderMgmt.number + 1 }}/{{ Math.max(orderMgmt.totalPages, 1) }} 页</div>
      </div>
    </section>

    <!-- 物流健康 -->
    <section class="admin-section" v-if="tab === 'health'">
      <div class="admin-kpis">
        <div class="admin-kpi glass accent">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">LIVE</span></div>
          <div class="admin-kpi__num mono">{{ orders.length }}<span class="admin-kpi__unit">单</span></div>
          <div class="admin-kpi__label">在管订单</div>
        </div>
        <div class="admin-kpi glass">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">TRANSIT</span></div>
          <div class="admin-kpi__num mono">{{ transitCount }}<span class="admin-kpi__unit">单</span></div>
          <div class="admin-kpi__label">在途订单</div>
        </div>
        <div class="admin-kpi glass">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">CSAT</span></div>
          <div class="admin-kpi__num mono">{{ approval?.rate != null ? (approval.rate * 100).toFixed(0) : '—' }}<span class="admin-kpi__unit">%</span></div>
          <div class="admin-kpi__label">好评率</div>
        </div>
      </div>
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">订单实时状态（前 {{ orders.length }} 条）</span>
          <el-button size="small" @click="loadHealth">刷新</el-button>
        </div>
        <el-table :data="orders" class="admin-table">
          <el-table-column prop="id" label="订单" width="80" class-name="mono" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="orderStatusTagType(row.status)">{{ orderStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100" class-name="mono">
            <template #default="{ row }">¥{{ fmtMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="责任员工" width="100" class-name="mono">
            <template #default="{ row }">员工 {{ row.staffId }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" class-name="mono" />
        </el-table>
      </div>
    </section>

    <!-- 用户管理 -->
    <section class="admin-section" v-if="tab === 'users'">
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">用户列表</span>
          <el-button size="small" @click="loadUsers">刷新</el-button>
        </div>
        <el-table :data="users.content" class="admin-table">
          <el-table-column prop="id" label="ID" width="60" class-name="mono" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column label="角色" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="roleTagType(row.role)">{{ roleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="toggleUser(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <!-- 员工管理 -->
    <section class="admin-section" v-if="tab === 'staff'">
      <div class="admin-tableglass glass" style="margin-bottom:14px">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">新增员工（用户角色将升为 STAFF）</span>
        </div>
        <div class="admin-actrow">
          <el-select v-model="staffForm.userId" filterable placeholder="搜索用户（姓名/ID）" size="small" style="width:220px" :filter-method="filterUser" :loading="userSearchLoading">
            <el-option v-for="u in userSearchResults" :key="u.id" :label="u.name + ' · 用户 ' + u.id + ' · ' + (u.role === 'STAFF' ? '已是员工' : roleLabel(u.role))" :value="u.id" :disabled="u.role === 'STAFF'" />
          </el-select>
          <el-input v-model="staffForm.licenseNo" placeholder="执业证件号" size="small" style="width:160px" />
          <el-input-number v-model="staffForm.siteId" :min="1" placeholder="站点ID" size="small" style="width:100px" controls-position="right" />
          <el-button size="small" type="primary" @click="createStaff">添加</el-button>
        </div>
      </div>
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">员工列表</span>
          <el-button size="small" @click="loadStaff">刷新</el-button>
        </div>
        <el-table :data="staffList" class="admin-table">
          <el-table-column prop="id" label="ID" width="60" class-name="mono" />
          <el-table-column prop="userId" label="用户ID" width="90" class-name="mono" />
          <el-table-column prop="siteId" label="站点" width="80" class-name="mono" />
          <el-table-column label="证件号" width="160" class-name="mono">
            <template #default="{ row }">{{ row.licenseNo ? maskLicense(row.licenseNo) : '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '在职' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="toggleStaff(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 员工申请 -->
      <div class="admin-tableglass glass" style="margin-top:14px">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">员工申请</span>
          <el-button size="small" @click="loadApplications">刷新</el-button>
        </div>
        <el-table :data="applications" class="admin-table">
          <el-table-column prop="id" label="ID" width="60" class-name="mono" />
          <el-table-column prop="userId" label="用户ID" width="90" class-name="mono" />
          <el-table-column prop="siteId" label="站点" width="80" class-name="mono" />
          <el-table-column label="证件号" width="160" class-name="mono">
            <template #default="{ row }">{{ row.licenseNo }}</template>
          </el-table-column>
          <el-table-column label="说明" min-width="160">
            <template #default="{ row }">{{ row.reason || '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'">
                {{ row.status === 'PENDING' ? '待审批' : row.status === 'APPROVED' ? '已通过' : '已拒绝' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button size="small" type="success" @click="approveApp(row)">通过</el-button>
                <el-button size="small" type="danger" @click="rejectApp(row)">拒绝</el-button>
              </template>
              <span v-else class="admin-readonly">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <!-- 公告管理 -->
    <section class="admin-section" v-if="tab === 'notices'">
      <div class="admin-notice-create glass" style="margin-bottom:14px">
        <el-input v-model="noticeForm.title" placeholder="标题" style="margin-bottom:8px" />
        <el-input v-model="noticeForm.body" placeholder="正文" type="textarea" :rows="2" />
        <el-button type="primary" size="small" @click="createNotice" style="margin-top:8px">创建草稿</el-button>
      </div>
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">公告列表</span>
          <el-button size="small" @click="loadNotices">刷新</el-button>
        </div>
        <el-table :data="notices.content" class="admin-table">
          <el-table-column prop="id" label="ID" width="60" class-name="mono" />
          <el-table-column prop="title" label="标题" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small">{{ row.status === 1 ? '已发布' : row.status === 2 ? '已归档' : '草稿' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="置顶" width="70">
            <template #default="{ row }">
              <el-tag v-if="row.pinned" size="small" type="danger">置顶</el-tag>
              <span v-else class="mono" style="color:var(--text-low)">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="220">
            <template #default="{ row }">
              <div class="admin-actrow">
                <el-button v-if="row.status !== 1" size="small" type="primary" @click="publishNotice(row)">发布</el-button>
                <el-button v-if="row.status === 1" size="small" type="warning" @click="archiveNotice(row)">归档</el-button>
                <el-button size="small" @click="pinNotice(row, !row.pinned)">{{ row.pinned ? '取消置顶' : '置顶' }}</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <!-- 评论管理 -->
    <section class="admin-section" v-if="tab === 'reviews'">
      <div class="admin-kpis" style="grid-template-columns: repeat(2,1fr)">
        <div class="admin-kpi glass accent">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">CSAT</span></div>
          <div class="admin-kpi__num mono">{{ approval?.rate != null ? (approval.rate * 100).toFixed(0) : '—' }}<span class="admin-kpi__unit">%</span></div>
          <div class="admin-kpi__label">好评率（{{ approval?.positive ?? 0 }}/{{ approval?.total ?? 0 }}）</div>
        </div>
        <div class="admin-kpi glass">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">COUNT</span></div>
          <div class="admin-kpi__num mono">{{ reviews.length }}<span class="admin-kpi__unit">条</span></div>
          <div class="admin-kpi__label">评论总数</div>
        </div>
      </div>
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">用户评论</span>
          <el-button size="small" @click="loadReviews">刷新</el-button>
        </div>
        <el-table :data="reviews" class="admin-table">
          <el-table-column prop="id" label="ID" width="60" class-name="mono" />
          <el-table-column prop="orderId" label="订单" width="80" class-name="mono" />
          <el-table-column prop="rating" label="评分" width="70" />
          <el-table-column prop="content" label="内容" />
          <el-table-column prop="staffReply" label="员工回复" width="200" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="deleteReview(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <!-- 反馈管理 -->
    <section class="admin-section" v-if="tab === 'feedback'">
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">用户反馈</span>
          <el-button size="small" @click="loadFeedback">刷新</el-button>
        </div>
        <el-table :data="feedbackList" class="admin-table">
          <el-table-column prop="id" label="ID" width="60" class-name="mono" />
          <el-table-column prop="type" label="类型" width="90" />
          <el-table-column prop="content" label="内容" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'CLOSED' ? 'info' : 'warning'">{{ row.status === 'OPEN' ? '处理中' : '已关闭' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reply" label="回复" width="200" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'CLOSED'" size="small" @click="closeFeedback(row)">关闭</el-button>
              <el-button v-else size="small" @click="openFeedback(row)">重开</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <!-- 客服 IM -->
    <section class="admin-section" v-if="tab === 'im'">
      <div class="admin-im-layout">
        <div class="admin-tableglass glass admin-im-sessions">
          <div class="admin-tablehead"><span class="admin-tabletitle">会话</span></div>
          <ul class="admin-im-list">
            <li v-for="s in imSessions" :key="s.id" class="admin-im-item" :class="{ active: activeIm === s.id }" @click="openIm(s)">
              <div class="admin-im-item__head mono">会话 #{{ s.id }} · 用户 {{ s.userId }}</div>
              <div class="admin-im-item__status">{{ s.status === 'ACTIVE' ? '进行中' : '已关闭' }}</div>
            </li>
          </ul>
          <el-empty v-if="!imSessions.length" description="暂无会话" />
        </div>
        <div class="admin-tableglass glass admin-im-msgs" v-if="activeIm != null">
          <div class="admin-tablehead">
            <span class="admin-tabletitle">会话 #{{ activeIm }}</span>
            <el-button size="small" @click="closeIm">关闭会话</el-button>
          </div>
          <div class="admin-im-msgs">
            <div v-for="m in imMsgs" :key="m.id" class="admin-im-msg" :class="{ admin: m.senderRole === 'ADMIN' }">
              <span class="mono admin-im-msg__role">{{ m.senderRole === 'ADMIN' ? '客服' : '用户' }}</span>
              <span>{{ m.content }}</span>
            </div>
          </div>
          <el-input v-model="imInput" placeholder="回复用户…" @keyup.enter="sendIm" style="margin-top:10px" />
          <el-button type="primary" size="small" @click="sendIm" style="margin-top:6px">发送</el-button>
        </div>
      </div>
    </section>

    <!-- 仓储管理 -->
    <section class="admin-section" v-if="tab === 'warehouse'">
      <div class="admin-warehouse">
        <div class="admin-tableglass glass">
          <div class="admin-tablehead">
            <span class="admin-tabletitle">仓储站点</span>
            <el-button size="small" @click="loadWarehouse">刷新</el-button>
          </div>
          <el-table :data="whSites" class="admin-table">
            <el-table-column prop="id" label="ID" width="60" class-name="mono" />
            <el-table-column prop="code" label="编码" width="100" class-name="mono" />
            <el-table-column prop="name" label="站点" />
            <el-table-column prop="capacity" label="容量" width="90" class-name="mono" />
            <el-table-column label="在库" width="90" class-name="mono">
              <template #default="{ row }">{{ whStock[row.id] ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="150">
              <template #default="{ row }">
                <div class="admin-actrow">
                  <el-button size="small" type="primary" @click="selectWhSite(row.id)">出入库</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="whActiveSite" class="admin-tableglass glass">
          <div class="admin-tablehead">
            <span class="admin-tabletitle">站点 {{ whActiveSite }} 出入库记录</span>
            <el-input-number v-model="whQty" :min="1" :max="99" :controls="false" size="small" style="width:90px" />
            <el-button size="small" type="primary" @click="whInbound">入库</el-button>
            <el-button size="small" type="warning" @click="whOutbound">出库</el-button>
          </div>
          <el-table :data="whRecords" class="admin-table">
            <el-table-column prop="id" label="ID" width="70" class-name="mono" />
            <el-table-column label="订单" width="90" class-name="mono">
              <template #default="{ row }">{{ row.orderId ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="动作" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.action === 'IN' ? 'success' : 'warning'">{{ row.action === 'IN' ? '入库' : '出库' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" class-name="mono" />
            <el-table-column prop="occurredAt" label="时间" class-name="mono" />
          </el-table>
        </div>
      </div>
    </section>

    <!-- 路线管理 -->
    <section class="admin-section" v-if="tab === 'routes'">
      <div class="admin-kpis kpi-row">
        <div class="admin-kpi glass accent">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">总量</span></div>
          <div class="admin-kpi__num mono">{{ routeSnap?.summary.total ?? 0 }}<span class="admin-kpi__unit">条</span></div>
          <div class="admin-kpi__label">路线总数</div>
        </div>
        <div class="admin-kpi glass">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">均距</span></div>
          <div class="admin-kpi__num mono">{{ (routeSnap?.summary.avgDistanceKm ?? 0).toFixed(1) }}<span class="admin-kpi__unit">公里</span></div>
          <div class="admin-kpi__label">平均距离</div>
        </div>
        <div class="admin-kpi glass">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">预计时长</span></div>
          <div class="admin-kpi__num mono">{{ fmtEta(routeSnap?.summary.avgEtaSeconds ?? 0) }}</div>
          <div class="admin-kpi__label">平均预计时长</div>
        </div>
        <div class="admin-kpi glass">
          <div class="admin-kpi__top"><span class="admin-kpi__tag">策略</span></div>
          <div class="admin-kpi__num mono">{{ Object.keys(routeSnap?.summary.strategyDistribution ?? {}).length }}<span class="admin-kpi__unit">类</span></div>
          <div class="admin-kpi__label">策略种类</div>
        </div>
      </div>

      <div class="admin-route-mapwrap">
        <div ref="routeMapEl" class="admin-routemap"></div>
        <div v-if="activeRoute" class="admin-route-detail glass">
          <div class="admin-route-detail__head">
            <span>路线编号 #{{ activeRoute.taskId }}</span>
            <el-button size="small" @click="activeRoute = null">关闭</el-button>
          </div>
          <div class="admin-route-detail__grid">
            <div><span>订单</span>#{{ activeRoute.orderId }}</div>
            <div><span>策略</span>{{ strategyLabel(activeRoute.strategy) }}</div>
            <div><span>出发</span>{{ activeRoute.fromRegion || '—' }}</div>
            <div><span>到达</span>{{ activeRoute.toRegion || '—' }}</div>
            <div><span>距离</span>{{ activeRoute.distanceKm.toFixed(1) }} 公里</div>
            <div><span>预计</span>{{ fmtEta(activeRoute.etaSeconds) }}</div>
            <div><span>费用</span>¥{{ (activeRoute.costFen / 100).toFixed(2) }}</div>
            <div><span>创建</span>{{ activeRoute.createdAt }}</div>
          </div>
        </div>
      </div>

      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">距离 Top 10 路线（红色加粗）</span>
        </div>
        <el-table :data="routeSnap?.topByDistance ?? []" class="admin-table" @row-click="selectRoute">
          <el-table-column prop="taskId" label="ID" width="64" class-name="mono" />
          <el-table-column prop="orderId" label="订单" width="70" class-name="mono" />
          <el-table-column label="策略" width="90">
            <template #default="{ row }">{{ strategyLabel(row.strategy) }}</template>
          </el-table-column>
          <el-table-column label="出发" width="80">
            <template #default="{ row }">{{ row.fromRegion || '—' }}</template>
          </el-table-column>
          <el-table-column label="到达" width="80">
            <template #default="{ row }">{{ row.toRegion || '—' }}</template>
          </el-table-column>
          <el-table-column label="距离" width="90" class-name="mono">
            <template #default="{ row }">{{ (row.distanceKm as number).toFixed(1) }} 公里</template>
          </el-table-column>
          <el-table-column label="预计" width="90" class-name="mono">
            <template #default="{ row }">{{ fmtEta(row.etaSeconds as number) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">最近 {{ routeSnap?.recent.length ?? 0 }} 条路线任务</span>
          <el-button size="small" @click="loadRoutes">刷新</el-button>
        </div>
        <el-table :data="routeSnap?.recent ?? []" class="admin-table" @row-click="selectRoute">
          <el-table-column prop="taskId" label="ID" width="64" class-name="mono" />
          <el-table-column prop="orderId" label="订单" width="70" class-name="mono" />
          <el-table-column label="策略" width="90">
            <template #default="{ row }">{{ strategyLabel(row.strategy) }}</template>
          </el-table-column>
          <el-table-column label="出发" width="80">
            <template #default="{ row }">{{ row.fromRegion || '—' }}</template>
          </el-table-column>
          <el-table-column label="到达" width="80">
            <template #default="{ row }">{{ row.toRegion || '—' }}</template>
          </el-table-column>
          <el-table-column label="距离" width="90" class-name="mono">
            <template #default="{ row }">{{ (row.distanceKm as number).toFixed(1) }} 公里</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
        </el-table>
      </div>
    </section>

    <!-- 需求审核 -->
    <section class="admin-section" v-if="tab === 'audit'">
      <div class="admin-tableglass glass">
        <div class="admin-tablehead">
          <span class="admin-tabletitle">待审核需求</span>
          <span class="admin-tablehint mono">{{ pendingDemands.length }} 条</span>
        </div>
        <el-table :data="pendingDemands" class="admin-table">
          <el-table-column prop="id" label="需求ID" width="80" class-name="mono" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="onClose(row)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

import { adminApi, type StatsSnapshot, type NoticeDto, type ReviewDto, type FeedbackDto, type ImSessionDto, type ImMessageDto, type UserDto, type StaffDto, type RouteSnapshot, type RouteDto } from '@/api/admin'
import type { Demand, Page, Order, OrderStatus } from '@/types'
import * as echarts from 'echarts'

const router = useRouter()
const auth = useAuthStore()
const tab = ref<'stats' | 'profile' | 'orders' | 'health' | 'users' | 'staff' | 'notices' | 'reviews' | 'feedback' | 'im' | 'warehouse' | 'routes' | 'audit'>('stats')
const tabs: { key: typeof tab.value; label: string }[] = [
  { key: 'stats', label: '订单统计' },
  { key: 'health', label: '物流监控' },
  { key: 'orders', label: '订单管理' },
  { key: 'routes', label: '路线管理' },
  { key: 'warehouse', label: '仓储管理' },
  { key: 'audit', label: '需求审核' },
  { key: 'users', label: '用户管理' },
  { key: 'staff', label: '员工管理' },
  { key: 'notices', label: '公告管理' },
  { key: 'reviews', label: '评论管理' },
  { key: 'feedback', label: '反馈管理' },
  { key: 'im', label: '在线客服' },
  { key: 'profile', label: '个人中心' },
]
const navGroups = computed(() => [
  { name: '监控', items: tabs.filter((t) => ['stats', 'health', 'orders', 'routes', 'warehouse', 'audit'].includes(t.key as string)) },
  { name: '管理', items: tabs.filter((t) => ['users', 'staff', 'notices', 'reviews', 'feedback', 'im'].includes(t.key as string)) },
  { name: '账户', items: tabs.filter((t) => (t.key as string) === 'profile') },
])
const navIcons: Record<string, Array<{ d?: string; c?: number[]; r?: number; w?: number; fill?: boolean }>> = {
  stats: [{ d: 'M4 20V10m6 10V4m6 16v-7m4 7H2' }],
  profile: [
    { c: [12, 8], r: 4 },
    { d: 'M4 21c1.5-4 5-5 8-5s6.5 1 8 5' },
  ],
  orders: [
    { d: 'M6.5 4.5h11A2.5 2.5 0 0 1 20 7v10a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4 17V7a2.5 2.5 0 0 1 2.5-2.5z' },
    { d: 'M8 10.5h8M8 14h5' },
  ],
  health: [{ d: 'M3 12h4l3-8 4 16 3-8h4' }],
  users: [
    { c: [9, 8], r: 3.4 },
    { d: 'M3 20c1.2-3.2 3.8-4.2 6-4.2s4.8 1 6 4.2' },
  ],
  staff: [
    { c: [12, 7.5], r: 3.4 },
    { d: 'M5.5 20.5c1.4-3.6 4-4.6 6.5-4.6s5.1 1 6.5 4.6' },
    { d: 'M15 4l4-2' },
  ],
  notices: [{ d: 'M5 9a7 7 0 0 1 14 0c0 5 2 6.5 2 6.5H3S5 14 5 9zm4.5 9a2.5 2.5 0 0 0 5 0' }],
  reviews: [
    { d: 'M5 5h14v9H8l-3 4V5z' },
    { d: 'M9 9.5h6' },
  ],
  feedback: [
    { d: 'M4 5h16v11H9l-4 4V5z' },
    { d: 'M8 9.5h8M8 12.5h5' },
  ],
  im: [
    { d: 'M4 14a8 8 0 1 1 16 0' },
    { c: [12, 14], r: 1.6, fill: true },
    { d: 'M4.5 14h3M16.5 14h3' },
  ],
  warehouse: [
    { d: 'M4 9l8-4.5L20 9v11H4V9z' },
    { d: 'M9 20v-6h6v6' },
  ],
  routes: [
    { c: [6, 6], r: 2.2 },
    { c: [18, 18], r: 2.2 },
    { d: 'M8 7.5C14 10 10 14 16.5 16.5' },
  ],
  audit: [
    { d: 'M12 3l7 4v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V7l7-4z' },
    { d: 'M9 12l2 2 4-4.5' },
  ],
}
const activeDesc = computed(() => {
  const d: Record<string, string> = {
    stats: '月度订单量、成交金额与热门路线的可视化总览',
    profile: '账号信息维护与安全设置',
    orders: '全平台订单的全维度筛选、查询与纠纷处理',
    health: '订单实时状态、在途运力与服务质量监控',
    users: '注册用户查询、注册审核与违规处置',
    staff: '员工账号管理、资质档案与入职申请审批',
    notices: '系统公告的创建、发布、置顶与归档',
    reviews: '用户评论的查看、删除与服务质量统计',
    feedback: '留言反馈的分类处理与回复跟踪',
    im: '用户会话接待与响应效率监控',
    warehouse: '仓储站点分布、库存状态与出入库记录',
    routes: 'GIS 路线资源查看、算法质量与维护',
    audit: '需求合规审核与违规需求下架',
  }
  return d[tab.value] ?? ''
})
const stats = ref<StatsSnapshot | null>(null)
const pendingDemands = ref<Demand[]>([])
const orders = ref<Order[]>([])
const approval = ref<{ total: number; positive: number; rate: number } | null>(null)
const users = ref<Page<UserDto>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })
const staffList = ref<StaffDto[]>([])
const applications = ref<import('@/api/admin').StaffApplicationDto[]>([])
const appActionBusy = ref(false)

async function loadApplications() {
  try { applications.value = await adminApi.staffApplications() } catch { applications.value = [] }
}

async function approveApp(row: import('@/api/admin').StaffApplicationDto) {
  if (!confirm('确认通过该申请？用户角色将升为 STAFF。')) return
  appActionBusy.value = true
  try {
    await adminApi.approveApplication(row.id)
    ElMessage.success('已批准，用户已生成员工档案')
    loadApplications()
    loadStaff()
  loadApplications()
    loadUsers()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '审批失败')
  } finally { appActionBusy.value = false }
}

async function rejectApp(row: import('@/api/admin').StaffApplicationDto) {
  const reason = prompt('拒绝原因（可选）：')
  if (reason === null) return
  appActionBusy.value = true
  try {
    await adminApi.rejectApplication(row.id, reason || undefined)
    ElMessage.success('已拒绝该申请')
    loadApplications()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '拒绝失败')
  } finally { appActionBusy.value = false }
}
const notices = ref<Page<NoticeDto>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })
const noticeForm = reactive({ title: '', body: '' })
const reviews = ref<ReviewDto[]>([])
const feedbackList = ref<FeedbackDto[]>([])
const imSessions = ref<ImSessionDto[]>([])
const activeIm = ref<number | null>(null)
const imMsgs = ref<ImMessageDto[]>([])
const imInput = ref('')

const totalOrders = computed(() => (stats.value?.monthly ?? []).reduce((s, m) => s + m.orderCount, 0))
const totalFen = computed(() => (stats.value?.monthly ?? []).reduce((s, m) => s + m.totalAmountFen, 0))

const monthlyChartEl = ref<HTMLDivElement | null>(null)
const regionChartEl = ref<HTMLDivElement | null>(null)
const topRoutesChartEl = ref<HTMLDivElement | null>(null)
let monthlyChart: echarts.ECharts | null = null
let regionChart: echarts.ECharts | null = null
let topRoutesChart: echarts.ECharts | null = null

const CHART_COLORS = ['#1e3a5f', '#3f6f9f', '#6f9fc4']
const CHART_TEXT = '#1e3a5f'
const CHART_LINE = 'rgba(30, 58, 95, 0.28)'
const CHART_FONT = '"Inter", "PingFang SC", sans-serif'

function renderMonthlyChart() {
  const el = monthlyChartEl.value
  if (!el || el.clientWidth === 0 || el.clientHeight === 0) return false
  if (!monthlyChart) monthlyChart = echarts.init(el)
  else monthlyChart.resize()
  const data = stats.value?.monthly ?? []
  monthlyChart.setOption({
    grid: { left: 40, right: 20, top: 40, bottom: 40 },
    legend: { data: ['订单量', '总金额(元)'], textStyle: { color: CHART_TEXT, fontFamily: CHART_FONT } },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.month),
      axisLine: { lineStyle: { color: CHART_LINE } },
      axisLabel: { color: CHART_TEXT, fontFamily: CHART_FONT },
    },
    yAxis: [
      { type: 'value', name: '订单量', splitLine: { lineStyle: { color: CHART_LINE } }, axisLabel: { color: CHART_TEXT } },
      { type: 'value', name: '金额(元)', splitLine: { show: false }, axisLabel: { color: CHART_TEXT } },
    ],
    series: [
      {
        name: '订单量',
        type: 'bar',
        data: data.map((d) => d.orderCount),
        itemStyle: { color: CHART_COLORS[0], borderRadius: [4, 4, 0, 0] },
      },
      {
        name: '总金额(元)',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: data.map((d) => (d.totalAmountFen / 100).toFixed(2)),
        itemStyle: { color: CHART_COLORS[2] },
        lineStyle: { color: CHART_COLORS[2], width: 2 },
      },
    ],
  })
  return true
}

function renderRegionChart() {
  const el = regionChartEl.value
  if (!el || el.clientWidth === 0 || el.clientHeight === 0) return false
  if (!regionChart) regionChart = echarts.init(el)
  else regionChart.resize()
  const data = [...(stats.value?.byRegion ?? [])].sort((a, b) => b.orderCount - a.orderCount).slice(0, 12)
  regionChart.setOption({
    grid: { left: 60, right: 30, top: 20, bottom: 40 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: CHART_LINE } },
      axisLabel: { color: CHART_TEXT },
    },
    yAxis: {
      type: 'category',
      data: data.map((d) => d.region).reverse(),
      axisLine: { lineStyle: { color: CHART_LINE } },
      axisLabel: { color: CHART_TEXT, fontFamily: CHART_FONT },
    },
    series: [
      {
        name: '订单量',
        type: 'bar',
        data: data.map((d) => d.orderCount).reverse(),
        itemStyle: { color: CHART_COLORS[1], borderRadius: [0, 3, 3, 0] },
        barMaxWidth: 18,
      },
    ],
  })
  return true
}

function renderTopRoutesChart() {
  const el = topRoutesChartEl.value
  if (!el || el.clientWidth === 0 || el.clientHeight === 0) return false
  if (!topRoutesChart) topRoutesChart = echarts.init(el)
  else topRoutesChart.resize()
  const routes = Object.entries(stats.value?.topRoutes ?? {}).sort((a, b) => b[1] - a[1]).slice(0, 10)
  topRoutesChart.setOption({
    grid: { left: 20, right: 30, top: 30, bottom: 60 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: routes.map(([r]) => r),
      axisLabel: { color: CHART_TEXT, fontFamily: CHART_FONT, interval: 0, rotate: 24 },
      axisLine: { lineStyle: { color: CHART_LINE } },
    },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: CHART_LINE } }, axisLabel: { color: CHART_TEXT } },
    series: [
      {
        name: '使用次数',
        type: 'line',
        data: routes.map(([, c]) => c),
        smooth: true,
        areaStyle: { color: 'rgba(30, 58, 95, 0.10)' },
        lineStyle: { color: CHART_COLORS[0], width: 2 },
        itemStyle: { color: CHART_COLORS[0] },
        symbolSize: 6,
      },
    ],
  })
  return true
}

function onResize() {
  monthlyChart?.resize()
  regionChart?.resize()
  topRoutesChart?.resize()
}

let chartPollTimer: number | null = null
let chartAttempts = 0

function stopChartPoll() {
  if (chartPollTimer != null) {
    clearTimeout(chartPollTimer)
    chartPollTimer = null
  }
}

function startChartPoll() {
  stopChartPoll()
  chartAttempts = 0
  const tick = () => {
    chartAttempts++
    const ok = renderMonthlyChart() && renderRegionChart() && renderTopRoutesChart()
    if (ok) {
      stopChartPoll()
      return
    }
    if (chartAttempts < 40) {
      chartPollTimer = window.setTimeout(tick, 100)
    } else {
      stopChartPoll()
    }
  }
  chartPollTimer = window.setTimeout(tick, 50)
}

watch(stats, () => {
  if (tab.value === 'stats') startChartPoll()
})

const sectionEl = ref<HTMLElement | null>(null)
watch(tab, async (t) => {
  if (t === 'stats') {
    // DOM was destroyed by v-if when we left; re-create chart instances
    // 先 dispose 旧实例并置 null，让 renderXxx 走 init 分支重新绑定 DOM
    monthlyChart?.dispose(); monthlyChart = null
    regionChart?.dispose(); regionChart = null
    topRoutesChart?.dispose(); topRoutesChart = null
    await nextTick()
    startChartPoll()
  } else {
    // 离开 stats 时 dispose，避免实例挂在已销毁的 DOM 上
    monthlyChart?.dispose(); monthlyChart = null
    regionChart?.dispose(); regionChart = null
    topRoutesChart?.dispose(); topRoutesChart = null
  }
})

onMounted(async () => {
  await nextTick()
  if (tab.value === 'stats') startChartPoll()
})

const profile = ref<{ id: number; name: string; role: string; phoneMasked: string } | null>(null)
const editName = ref('')
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

async function loadProfile() {
  try {
    const p = await adminApi.adminProfile()
    profile.value = { id: p.id, name: p.name, role: p.role, phoneMasked: p.phoneMasked }
    editName.value = p.name
  } catch (e) {
    console.error(e)
  }
}

async function saveProfileName() {
  if (!editName.value.trim()) return
  try {
    const p = await adminApi.updateAdminProfile(editName.value.trim())
    if (profile.value) profile.value = { ...profile.value, name: p.name }
    ElMessage.success('姓名已更新')
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

async function savePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) return
  try {
    await adminApi.changeAdminPassword(pwdForm.oldPassword, pwdForm.newPassword)
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    ElMessage.success('密码已更新')
  } catch (e) {
    ElMessage.error('密码修改失败')
  }
}

onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  monthlyChart?.dispose(); monthlyChart = null
  regionChart?.dispose(); regionChart = null
  topRoutesChart?.dispose(); topRoutesChart = null
})
const transitCount = computed(() => orders.value.filter((o) => o.status === 'IN_TRANSIT' || o.status === 'PICKED').length)

const whSites = ref<import('@/api/admin').WarehouseSiteDto[]>([])
const whStock = ref<Record<number, number>>({})
const whRecords = ref<import('@/api/admin').WarehouseRecordDto[]>([])
const whActiveSite = ref<number | null>(null)
const whQty = ref(1)

async function loadWarehouse() {
  try {
    whSites.value = await adminApi.warehouseSites()
    for (const site of whSites.value) {
      whStock.value[site.id] = await adminApi.warehouseStock(site.id)
    }
  } catch (e) {
    console.error(e)
  }
}
async function selectWhSite(siteId: number) {
  whActiveSite.value = siteId
  try {
    whRecords.value = await adminApi.warehouseRecords(siteId)
  } catch (e) {
    console.error(e)
  }
}
async function whInbound() {
  if (!whActiveSite.value) return
  try {
    await adminApi.warehouseInbound(whActiveSite.value, undefined, whQty.value)
    await Promise.all([loadWarehouse(), selectWhSite(whActiveSite.value)])
    ElMessage.success('入库成功')
  } catch (e) {
    ElMessage.error('入库失败：' + (e instanceof Error ? e.message : '未知错误'))
  }
}
async function whOutbound() {
  if (!whActiveSite.value) return
  try {
    await adminApi.warehouseOutbound(whActiveSite.value, undefined, whQty.value)
    await Promise.all([loadWarehouse(), selectWhSite(whActiveSite.value)])
    ElMessage.success('出库成功')
  } catch (e) {
    ElMessage.error('出库失败：' + (e instanceof Error ? e.message : '未知错误'))
  }
}

const userOptions = computed(() => users.value.content.map((u) => ({ id: u.id, label: '用户 ' + u.id + ' · ' + (u.name || '') })))
const orderStatusOptions = [
  { value: 'PENDING', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'PICKED', label: '已取件' },
  { value: 'IN_TRANSIT', label: '配送中' },
  { value: 'ARRIVED', label: '已到达' },
  { value: 'DELIVERED', label: '已送达' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REFUNDING', label: '退款中' },
  { value: 'REFUNDED', label: '已退款' },
] as const
const orderFilter = reactive({ status: undefined as OrderStatus | undefined, staffId: undefined as number | undefined, userId: undefined as number | undefined, dateRange: [] as string[] })
const orderMgmt = ref<Page<Order>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })
const routeSnap = ref<RouteSnapshot | null>(null)
const activeRoute = ref<RouteDto | null>(null)
const routeMapEl = ref<HTMLDivElement | null>(null)
let routeMap: import('maplibre-gl').Map | null = null

async function loadRoutes() {
  try {
    routeSnap.value = await adminApi.routes(20)
    await nextTick()
    renderRouteMap()
  } catch (e) {
    console.error(e)
  }
}

async function renderRouteMap() {
  if (!routeMapEl.value) return
  if (!routeMap) {
    const maplibre = await import('maplibre-gl')
    // 高德 raster 瓦片（与 GisMap.vue 一致，OSM 在国内不可达会白底）
    const autonaviTiles = [
      'https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
      'https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
      'https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
      'https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
    ]
    routeMap = new maplibre.Map({
      container: routeMapEl.value,
      style: {
        version: 8,
        sources: { autonavi: { type: 'raster', tiles: autonaviTiles, tileSize: 256, attribution: '© 高德' } },
        layers: [
          { id: 'bg', type: 'background', paint: { 'background-color': '#e8edf3' } },
          { id: 'autonavi-layer', type: 'raster', source: 'autonavi', paint: { 'raster-opacity': 0.9, 'raster-saturation': 0 } },
        ],
      },
      center: [108.6, 35.2],
      zoom: 5,
    })
    routeMap.on('load', () => {
      const topIds = new Set((routeSnap.value?.topByDistance ?? []).map((r) => r.taskId))
      const lines: Record<string, unknown>[] = []
      for (const r of routeSnap.value?.recent ?? []) {
        const coords = (r.geometry ?? []).map((p: number[]) => [p[0], p[1]])
        if (coords.length >= 2) {
          lines.push({
            type: 'Feature',
            properties: { taskId: r.taskId, distanceKm: r.distanceKm, strategy: r.strategy, top: topIds.has(r.taskId) },
            geometry: { type: 'LineString', coordinates: coords },
          })
        }
      }
      const points: Record<string, unknown>[] = []
      for (const r of routeSnap.value?.recent ?? []) {
        const g = (r.geometry ?? []).map((p: number[]) => [p[0], p[1]])
        if (g.length >= 2) {
          points.push({ type: 'Feature', properties: { name: r.fromRegion || '起点', kind: 'from' }, geometry: { type: 'Point', coordinates: g[0] } })
          points.push({ type: 'Feature', properties: { name: r.toRegion || '终点', kind: 'to' }, geometry: { type: 'Point', coordinates: g[g.length - 1] } })
        }
      }
      routeMap!.addSource('routes', { type: 'geojson', data: { type: 'FeatureCollection', features: lines } as never })
      routeMap!.addSource('route-points', { type: 'geojson', data: { type: 'FeatureCollection', features: points } as never })
      routeMap!.addLayer({ id: 'routes-line', type: 'line', source: 'routes', paint: { 'line-color': '#1e3a5f', 'line-width': 2.5, 'line-opacity': 0.55, 'line-dasharray': [3, 2] } })
      routeMap!.addLayer({ id: 'routes-line-top', type: 'line', source: 'routes', filter: ['==', ['get', 'top'], true], paint: { 'line-color': '#c0392b', 'line-width': 4, 'line-opacity': 0.9, 'line-dasharray': [4, 2] } })
      routeMap!.addLayer({ id: 'route-point-circles', type: 'circle', source: 'route-points', paint: { 'circle-radius': 5, 'circle-color': ['case', ['==', ['get', 'kind'], 'to'], '#c0392b', '#1e3a5f'], 'circle-stroke-color': '#ffffff', 'circle-stroke-width': 1.5 } })
      routeMap!.addLayer({
        id: 'route-point-labels',
        type: 'symbol',
        source: 'route-points',
        layout: {
          'text-field': ['get', 'name'],
          'text-size': 13,
          'text-offset': [0, 1.8],
          'text-anchor': 'top',
        },
        paint: {
          'text-color': '#18202b',
          'text-halo-color': '#ffffff',
          'text-halo-width': 1.8,
          'text-halo-blur': 0.6,
        },
      })
    })
  }
}

async function selectRoute(r: RouteDto) {
  activeRoute.value = r
  renderRouteMap()
}

async function loadOrderMgmt() {
  try {
    const from = orderFilter.dateRange[0] || undefined
    const to = orderFilter.dateRange[1] ? orderFilter.dateRange[1] + ' 23:59:59' : undefined
    orderMgmt.value = await adminApi.orders(orderFilter.status, orderFilter.staffId, orderFilter.userId, from, to, 0, 50)
  } catch (e) {
    console.error(e)
  }
}

function fmtMoney(fen: number) { return (fen / 100).toFixed(2) }

const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING: '待支付', PAID: '已支付', PICKED: '已揽件', IN_TRANSIT: '配送中',
  ARRIVED: '已到达', DELIVERED: '已送达', CANCELLED: '已取消',
  REFUNDING: '退款中', REFUNDED: '已退款',
}
function orderStatusLabel(s: string): string { return ORDER_STATUS_LABEL[s] ?? s }
function orderStatusTagType(s: string): 'success' | 'warning' | 'danger' | 'info' {
  if (s === 'DELIVERED') return 'success'
  if (s === 'CANCELLED' || s === 'REFUNDED') return 'danger'
  if (s === 'IN_TRANSIT' || s === 'PICKED' || s === 'ARRIVED') return 'warning'
  return 'info'
}

const STRATEGY_LABEL: Record<string, string> = { FASTEST: '最快时效', SHORTEST: '最短距离', LOWEST_COST: '最低成本' }
function strategyLabel(s: string): string { return STRATEGY_LABEL[s] ?? s }

const ROLE_LABEL: Record<string, string> = { USER: '普通用户', STAFF: '配送员工', ADMIN: '管理员' }
function roleLabel(r: string): string { return ROLE_LABEL[r] ?? r }
function roleTagType(r: string): 'success' | 'warning' | 'danger' | 'info' {
  if (r === 'ADMIN') return 'danger'
  if (r === 'STAFF') return 'warning'
  return 'info'
}

function maskLicense(no: string): string {
  if (no.length <= 4) return no
  return no.slice(0, 2) + '****' + no.slice(-2)
}
function fmtEta(seconds: number): string {
  if (seconds <= 0) return '—'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

onMounted(loadAll)

async function loadAll() {
  await Promise.all([loadStats(), loadHealth(), loadProfile(), loadOrderMgmt(), loadUsers(), loadStaff(), loadNotices(), loadReviews(), loadFeedback(), loadImSessions(), loadWarehouse(), loadRoutes(), loadAudit(), loadApplications()])
}

async function loadStats() {
  try { stats.value = await adminApi.orderStats(12) } catch { stats.value = null }
}
async function loadHealth() {
  try {
    const [o, r] = await Promise.all([adminApi.orders(undefined, undefined, undefined, undefined, undefined, 0, 50), adminApi.approvalRate()])
    orders.value = o.content
    approval.value = r
  } catch (e) { console.error(e) }
}
async function loadUsers() { try { users.value = await adminApi.users(0, 50) } catch (e) { console.error(e) } }
const staffForm = reactive({ userId: undefined as number | undefined, licenseNo: '', siteId: undefined as number | undefined })
const userSearchResults = ref<UserDto[]>([])
const userSearchLoading = ref(false)

async function filterUser(query: string) {
  if (!query || query.length < 1) {
    userSearchResults.value = []
    return
  }
  userSearchLoading.value = true
  try {
    const p = await adminApi.users(0, 200)
    const kw = query.toLowerCase()
    userSearchResults.value = p.content.filter((u) => u.name?.toLowerCase().includes(kw) || String(u.id) === kw)
  } catch {
    userSearchResults.value = []
  } finally {
    userSearchLoading.value = false
  }
}

async function createStaff() {
  if (!staffForm.userId) { ElMessage.warning('请选择用户'); return }
  try {
    await adminApi.addStaff({ userId: staffForm.userId, siteId: staffForm.siteId ?? 1, licenseNo: staffForm.licenseNo || 'N/A' })
    ElMessage.success('已添加员工，该用户角色已升为 STAFF')
    staffForm.userId = undefined; staffForm.licenseNo = ''; staffForm.siteId = undefined
    userSearchResults.value = []
    loadStaff()
    loadUsers()
  } catch (e) {
    ElMessage.error('添加失败，该用户可能已是员工')
  }
}

async function loadStaff() { try { staffList.value = await adminApi.staff() } catch (e) { console.error(e) } }
async function loadNotices() { try { notices.value = await adminApi.notices(undefined, 0, 50) } catch (e) { console.error(e) } }
async function loadReviews() { try { reviews.value = await adminApi.reviews(); approval.value = await adminApi.approvalRate() } catch (e) { console.error(e) } }
async function loadFeedback() { try { feedbackList.value = await adminApi.feedback() } catch (e) { console.error(e) } }
async function loadImSessions() { try { imSessions.value = await adminApi.imSessions() } catch (e) { console.error(e) } }
async function loadAudit() { try { const p = await adminApi.auditDemands(); pendingDemands.value = p.content } catch (e) { console.error(e) } }

async function toggleUser(row: UserDto) {
  try { await adminApi.setUserStatus(row.id, row.status === 1 ? 0 : 1); ElMessage.success('已更新'); loadUsers() } catch (e) { ElMessage.error('操作失败') }
}
async function toggleStaff(row: StaffDto) {
  try { await adminApi.setStaffStatus(row.id, row.status === 1 ? 0 : 1); ElMessage.success('已更新'); loadStaff() } catch (e) { ElMessage.error('操作失败') }
}
async function createNotice() {
  if (!noticeForm.title) return ElMessage.warning('请填写标题')
  try { await adminApi.createNotice(noticeForm.title, noticeForm.body); ElMessage.success('已创建'); noticeForm.title=''; noticeForm.body=''; loadNotices() } catch { ElMessage.error('创建失败') }
}
async function publishNotice(row: NoticeDto) { try { await adminApi.publishNotice(row.id); ElMessage.success('已发布'); loadNotices() } catch { ElMessage.error('发布失败') } }
async function pinNotice(row: NoticeDto, pinned: boolean) {
  try {
    await adminApi.pinNotice(row.id, pinned)
    await loadNotices()
    ElMessage.success(pinned ? '已置顶' : '已取消置顶')
  } catch (e) {
    ElMessage.error((e as Error).message || '操作失败')
  }
}

async function archiveNotice(row: NoticeDto) { try { await adminApi.archiveNotice(row.id); ElMessage.success('已归档'); loadNotices() } catch { ElMessage.error('归档失败') } }
async function deleteReview(row: ReviewDto) { try { await adminApi.deleteReview(row.id); ElMessage.success('已删除'); loadReviews() } catch { ElMessage.error('删除失败') } }
async function closeFeedback(row: FeedbackDto) { try { await adminApi.processFeedback(row.id, 'CLOSED'); ElMessage.success('已关闭'); loadFeedback() } catch { ElMessage.error('操作失败') } }
async function openFeedback(row: FeedbackDto) { try { await adminApi.processFeedback(row.id, 'OPEN'); ElMessage.success('已重开'); loadFeedback() } catch { ElMessage.error('操作失败') } }
async function openIm(s: ImSessionDto) {
  activeIm.value = s.id
  try { imMsgs.value = await adminApi.imMessages(s.id) } catch { imMsgs.value = [] }
}
async function sendIm() {
  if (!imInput.value.trim() || activeIm.value == null) return
  ElMessage.info('客服回复消息接口待后端开放（管理端 IM 回复）')
  imInput.value = ''
}
async function closeIm() {
  if (activeIm.value == null) return
  try { await adminApi.imClose(activeIm.value); ElMessage.success('会话已关闭'); activeIm.value=null; loadImSessions() } catch { ElMessage.error('关闭失败') }
}
async function onClose(row: Demand) {
  try { await adminApi.closeDemand(row.id, '管理员下架'); ElMessage.success('已下架'); pendingDemands.value = pendingDemands.value.filter((d) => d.id !== row.id) } catch (e) { ElMessage.error(e instanceof Error ? e.message : '下架失败') }
}
function logout() { auth.logout(); router.push('/user/login') }
</script>

<style scoped>
/* ============================================================
 * 管理控制台 · 运营指挥台重设计
 * 深海军蓝(#1e3a5f) 主色 + 琥珀(#d98e2b) 运营强调
 * 侧边命令导航 / Bento KPI / Stagger 入场 / 等宽数据字
 * 仅作用于本页局部，不影响全局令牌
 * ============================================================ */
.admin-wrap {
  --adm-navy: #1e3a5f;
  --adm-navy-deep: #142a45;
  --adm-navy-soft: #35577f;
  --adm-amber: #d98e2b;
  --adm-amber-soft: #f3e3c8;
  --adm-canvas: #f4f6f9;
  --adm-panel: #ffffff;
  --adm-line: rgba(30, 58, 95, 0.12);
  --adm-line-strong: rgba(30, 58, 95, 0.28);
  --adm-text-hi: #18202b;
  --adm-text-mid: #52606f;
  --adm-text-low: #8b98a8;
  min-height: calc(100vh - 108px);
}

/* ---------- 双栏壳：侧边命令导航 + 主内容 ---------- */
.admin-shell {
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}
.admin-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
@media (max-width: 1080px) {
  .admin-shell { grid-template-columns: 1fr; }
  .admin-nav {
    position: static;
    width: 100%;
    max-width: none;
    flex-direction: row;
    overflow-x: auto;
    scrollbar-width: none;
  }
  .admin-nav__brand { border-bottom: none; border-right: 1px solid var(--adm-line); }
  .admin-nav__groups { display: flex; gap: 18px; padding: 10px 14px; }
  .admin-nav__group { display: flex; gap: 4px; }
  .admin-nav__groupname { display: none; }
  .admin-nav__foot { border-top: none; border-left: 1px solid var(--adm-line); }
}

/* ---------- 侧边命令导航 ---------- */
.admin-nav {
  position: sticky;
  top: 76px;
  display: flex;
  flex-direction: column;
  width: 236px;
  max-height: calc(100vh - 100px);
  background: var(--adm-panel);
  border: 1px solid var(--adm-line);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(30, 58, 95, 0.06);
}
.admin-nav__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px 14px;
  border-bottom: 1px solid var(--adm-line);
  color: var(--adm-navy);
}
.admin-nav__mark { flex: 0 0 26px; }
.admin-nav__name { display: flex; flex-direction: column; gap: 2px; line-height: 1.15; }
.admin-nav__name strong { font-size: 14.5px; font-weight: 700; color: var(--adm-navy); letter-spacing: 0.02em; }
.admin-nav__name span { font-size: 9.5px; letter-spacing: 0.18em; color: var(--adm-text-low); }
.admin-nav__groups { flex: 1; overflow-y: auto; padding: 10px 0 6px; scrollbar-width: thin; }
.admin-nav__group + .admin-nav__group { margin-top: 10px; }
.admin-nav__groupname {
  padding: 8px 18px 4px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--adm-text-low);
}
.admin-navitem {
  display: flex;
  align-items: center;
  gap: 10px;
  width: calc(100% - 20px);
  margin: 1px 10px;
  padding: 8px 10px;
  border: none;
  border-left: 2px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--adm-text-mid);
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background 0.16s var(--ease), color 0.16s var(--ease), border-color 0.16s var(--ease);
}
.admin-navitem:hover { background: rgba(30, 58, 95, 0.055); color: var(--adm-text-hi); }
.admin-navitem.active {
  background: var(--adm-navy);
  border-left-color: var(--adm-amber);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 3px 10px rgba(30, 58, 95, 0.28);
}
.admin-navitem__icon { flex: 0 0 16px; opacity: 0.9; }
.admin-navitem__label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.admin-nav__foot {
  padding: 12px;
  border-top: 1px solid var(--adm-line);
  background: rgba(30, 58, 95, 0.03);
}
.admin-logout {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  background: transparent;
  border: 1px solid var(--adm-line-strong);
  color: var(--adm-text-mid);
  border-radius: 9px;
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: color 0.16s var(--ease), border-color 0.16s var(--ease), background 0.16s var(--ease);
}
.admin-logout:hover { color: #d64545; border-color: rgba(214, 69, 69, 0.5); background: rgba(214, 69, 69, 0.05); }

/* ---------- 模块页眉：大标题 + 琥珀分隔线 + 描述 ---------- */
.admin-pagehead {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px 16px;
  padding: 4px 2px 14px;
  border-bottom: 1px solid var(--adm-line);
}
.admin-pagehead__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--adm-navy);
  letter-spacing: 0.01em;
  display: flex;
  align-items: center;
  gap: 10px;
}
.admin-pagehead__title::before {
  content: "";
  width: 4px;
  height: 20px;
  border-radius: 2px;
  background: var(--adm-amber);
  box-shadow: 0 0 0 3px rgba(217, 142, 43, 0.18);
}
.admin-pagehead__desc {
  width: 100%;
  margin: 8px 0 0;
  font-size: 12.5px;
  color: var(--adm-text-mid);
}
.admin-pagehead__meta { font-size: 12px; color: var(--adm-text-low); }

/* ---------- 内容区：分组节奏 + Stagger 入场 ---------- */
.admin-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.admin-section > * {
  animation: adm-reveal 0.42s var(--ease) both;
}
.admin-section > :nth-child(2) { animation-delay: 0.06s; }
.admin-section > :nth-child(3) { animation-delay: 0.12s; }
.admin-section > :nth-child(4) { animation-delay: 0.18s; }
.admin-section > :nth-child(5) { animation-delay: 0.24s; }
@keyframes adm-reveal {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}
@media (prefers-reduced-motion: reduce) {
  .admin-section > * { animation: none; }
}

/* ---------- 面板：白底 + 海军蓝细边 + 双层软阴影 ---------- */
.admin-wrap .glass {
  background: var(--adm-panel);
  border: 1px solid var(--adm-line);
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(30, 58, 95, 0.05), 0 8px 24px rgba(30, 58, 95, 0.07);
}
.admin-tableglass { padding: 18px 20px; }
.admin-tablehead {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--adm-line-strong);
}
.admin-tabletitle {
  font-size: 15px;
  font-weight: 700;
  color: var(--adm-navy);
  display: flex;
  align-items: center;
  gap: 8px;
}
.admin-tabletitle::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--adm-amber);
  box-shadow: 0 0 0 2.5px rgba(217, 142, 43, 0.22);
}
.admin-tablehint { font-size: 12px; color: var(--adm-text-low); }
.admin-pager { margin-top: 12px; font-size: 12px; color: var(--adm-text-low); }
.admin-empty { color: var(--adm-text-low); font-size: 13px; padding: 8px 0; }
.admin-readonly { color: var(--adm-text-low); }

/* ---------- Bento KPI ---------- */
.admin-kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 14px;
}
.admin-kpis.kpi-row { grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); }
.admin-kpi {
  position: relative;
  padding: 18px 20px;
  overflow: hidden;
}
.admin-kpi::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--adm-navy), rgba(30, 58, 95, 0.25));
}
.admin-kpi.accent { background: var(--adm-navy); border-color: var(--adm-navy-deep); }
.admin-kpi.accent::before { background: var(--adm-amber); width: 4px; }
.admin-kpi.accent .admin-kpi__num { color: #fff; }
.admin-kpi.accent .admin-kpi__label { color: rgba(255, 255, 255, 0.66); }
.admin-kpi.accent .admin-kpi__unit { color: var(--adm-amber); }
.admin-kpi__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.admin-kpi__tag {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--adm-navy-soft);
  background: rgba(30, 58, 95, 0.07);
  padding: 3px 8px;
  border-radius: 5px;
}
.admin-kpi.accent .admin-kpi__tag { color: var(--adm-amber); background: rgba(217, 142, 43, 0.18); }
.admin-kpi__num {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  color: var(--adm-navy);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}
.admin-kpi__num .admin-kpi__unit {
  font-size: 15px;
  font-weight: 600;
  margin-left: 4px;
  color: var(--adm-amber);
}
.admin-kpi__label { margin-top: 9px; font-size: 12.5px; color: var(--adm-text-mid); }

/* ---------- 表格精修：海军蓝表头 + 斑马纹 ---------- */
.admin-table { width: 100%; }
.admin-wrap .admin-table :deep(.el-table__header th.el-table__cell) {
  background: rgba(30, 58, 95, 0.055) !important;
  color: var(--adm-navy) !important;
  font-weight: 700;
  font-size: 12.5px;
  letter-spacing: 0.02em;
  border-bottom: 1.5px solid var(--adm-line-strong) !important;
}
.admin-wrap .admin-table :deep(.el-table__row) td {
  border-bottom: 1px solid rgba(30, 58, 95, 0.06);
}
.admin-wrap .admin-table :deep(.el-table__row:nth-child(even)) td {
  background: rgba(30, 58, 95, 0.022);
}
.admin-wrap .admin-table :deep(.el-table__row:hover td) {
  background: rgba(30, 58, 95, 0.06) !important;
}

/* ---------- 操作行：按钮工整等宽 ---------- */
.admin-actrow {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.admin-actrow .el-button { min-width: 64px; }

/* ---------- 客服 IM ---------- */
.admin-im-layout { display: grid; grid-template-columns: 280px minmax(0, 1fr); gap: 14px; }
@media (max-width: 900px) { .admin-im-layout { grid-template-columns: 1fr; } }
.admin-im-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.admin-im-item {
  padding: 10px 12px;
  border: 1px solid var(--adm-line);
  border-left: 3px solid transparent;
  border-radius: 9px;
  cursor: pointer;
  transition: border-color 0.15s var(--ease), background 0.15s var(--ease);
}
.admin-im-item:hover { background: rgba(30, 58, 95, 0.04); }
.admin-im-item.active { border-left-color: var(--adm-amber); background: rgba(30, 58, 95, 0.06); }
.admin-im-item__head { font-size: 13px; font-weight: 600; color: var(--adm-text-hi); }
.admin-im-item__status { font-size: 11px; color: var(--adm-text-low); margin-top: 4px; }
.admin-im-msgs { display: flex; flex-direction: column; gap: 8px; max-height: 320px; overflow-y: auto; }
.admin-im-msg { padding: 8px 10px; border-radius: 9px; background: rgba(30, 58, 95, 0.05); font-size: 13px; }
.admin-im-msg.admin { background: var(--adm-navy); color: #fff; align-self: flex-end; }
.admin-im-msg.admin .admin-im-msg__role { color: var(--adm-amber-soft); }
.admin-im-msg__role { font-size: 10px; color: var(--adm-text-low); margin-right: 8px; }

/* ---------- 图表 Bento ---------- */
.admin-chartrow { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
@media (max-width: 900px) { .admin-chartrow { grid-template-columns: 1fr; } }
.admin-chartglass { min-height: 240px; padding: 18px 20px; }
.admin-chartbox { width: 100%; height: 230px; }
.admin-chartbox--tall { height: 280px; }

/* ---------- 个人中心：四宫格档案 + 琥珀编辑组 ---------- */
.admin-profile { padding: 2px 0; }
.admin-profile__row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 18px; }
.admin-profile__item {
  padding: 14px 14px;
  border: 1px solid var(--adm-line);
  border-radius: 10px;
  background: rgba(30, 58, 95, 0.028);
  transition: border-color 0.15s var(--ease);
}
.admin-profile__item:hover { border-color: var(--adm-line-strong); }
.admin-profile__label {
  font-size: 10.5px;
  color: var(--adm-text-low);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 7px;
}
.admin-profile__val { font-size: 16px; font-weight: 700; color: var(--adm-navy); }
.admin-profile__group { margin-top: 14px; padding-top: 14px; border-top: 1px dashed var(--adm-line-strong); }
.admin-profile__subhead {
  font-size: 12px;
  font-weight: 700;
  color: var(--adm-navy);
  letter-spacing: 0.06em;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.admin-profile__subhead::before { content: ""; width: 5px; height: 5px; border-radius: 50%; background: var(--adm-amber); }
.admin-profile__inline { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
@media (max-width: 900px) {
  .admin-profile__row { grid-template-columns: repeat(2, 1fr); }
}

/* ---------- 路线 GIS 区 ---------- */
.admin-route-mapwrap { position: relative; }
.admin-routemap {
  width: 100%;
  height: 380px;
  border-radius: 12px;
  border: 1px solid var(--adm-line-strong);
  overflow: hidden;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}
.admin-route-detail {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 300px;
  border-radius: 12px;
  padding: 14px 16px;
  background: #ffffff;
  color: #18202b;
  border: 1px solid rgba(30, 58, 95, 0.25);
  box-shadow: 0 10px 28px rgba(20, 42, 69, 0.18);
  font-size: 13px;
  z-index: 10;
}
.admin-route-detail__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(30, 58, 95, 0.12);
}
.admin-route-detail__head > span {
  font-size: 14px;
  font-weight: 700;
  color: #1e3a5f;
  font-family: var(--font-mono);
}
.admin-route-detail__grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 16px; }
.admin-route-detail__grid div span {
  display: block;
  font-size: 11px;
  color: #8a919c;
  margin-bottom: 3px;
}
.admin-route-detail__grid div {
  color: #18202b;
  font-size: 14px;
  font-weight: 600;
}

/* ---------- 公告创建 ---------- */
.admin-notice-create { padding: 16px 18px; }

/* ---------- 仓储 ---------- */
.admin-warehouse { display: flex; flex-direction: column; gap: 14px; }
</style>
