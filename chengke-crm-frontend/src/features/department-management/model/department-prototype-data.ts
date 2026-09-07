export type DepartmentStatus = 'active' | 'disabled';
export type DepartmentNodeType = 'group' | 'department';

export interface DepartmentLeader {
  initials: string;
  name: string;
  username: string;
}

export interface DepartmentNode {
  businessReferenceCount: number;
  children: DepartmentNode[];
  code: string;
  directMembers: number;
  id: string;
  leader: DepartmentLeader | null;
  name: string;
  nodeType: DepartmentNodeType;
  parentId: string | null;
  parentName: string | null;
  remark: string;
  roleCount: number;
  sortOrder: number;
  status: DepartmentStatus;
  updatedAt: string;
  updatedBy: string;
}

export interface DepartmentSearchResult {
  ancestorPath: string;
  node: DepartmentNode;
}

export const DEFAULT_DEPARTMENT_ID = 'east-division';

export const departmentPrototypeTree: DepartmentNode[] = [
  {
    id: 'chengke-group',
    parentId: null,
    parentName: null,
    name: '澄客集团',
    code: 'chengke_group',
    nodeType: 'group',
    status: 'active',
    leader: { name: '顾明远', username: 'gumingyuan', initials: '顾' },
    sortOrder: 0,
    directMembers: 8,
    roleCount: 5,
    businessReferenceCount: 12,
    remark: '澄客 CRM 当前租户的集团根节点。',
    updatedAt: '2026-08-09 18:24',
    updatedBy: '系统管理员',
    children: [
      {
        id: 'group-hr',
        parentId: 'chengke-group',
        parentName: '澄客集团',
        name: '集团人力资源部',
        code: 'group_hr',
        nodeType: 'department',
        status: 'active',
        leader: { name: '林知夏', username: 'linzhixia', initials: '林' },
        sortOrder: 100,
        directMembers: 12,
        roleCount: 1,
        businessReferenceCount: 3,
        remark: '负责集团招聘、员工关系与组织发展。',
        updatedAt: '2026-08-08 16:42',
        updatedBy: '林知夏',
        children: [],
      },
      {
        id: 'group-finance',
        parentId: 'chengke-group',
        parentName: '澄客集团',
        name: '集团财务部',
        code: 'group_finance',
        nodeType: 'department',
        status: 'active',
        leader: { name: '陈嘉言', username: 'chenjiayan', initials: '陈' },
        sortOrder: 200,
        directMembers: 18,
        roleCount: 1,
        businessReferenceCount: 8,
        remark: '负责集团预算、结算和经营分析。',
        updatedAt: '2026-08-09 09:18',
        updatedBy: '陈嘉言',
        children: [],
      },
      {
        id: 'east-division',
        parentId: 'chengke-group',
        parentName: '澄客集团',
        name: '华东事业部',
        code: 'east_division',
        nodeType: 'department',
        status: 'active',
        leader: { name: '周清和', username: 'zhouqinghe', initials: '周' },
        sortOrder: 300,
        directMembers: 16,
        roleCount: 2,
        businessReferenceCount: 27,
        remark: '负责华东区域销售团队与客户经营。',
        updatedAt: '2026-08-09 14:36',
        updatedBy: '赵敏',
        children: [
          {
            id: 'shanghai-sales-one',
            parentId: 'east-division',
            parentName: '华东事业部',
            name: '上海销售一部',
            code: 'shanghai_sales_1',
            nodeType: 'department',
            status: 'active',
            leader: { name: '陆思源', username: 'lusiyuan', initials: '陆' },
            sortOrder: 100,
            directMembers: 23,
            roleCount: 1,
            businessReferenceCount: 14,
            remark: '负责上海重点行业客户。',
            updatedAt: '2026-08-08 11:09',
            updatedBy: '陆思源',
            children: [],
          },
          {
            id: 'hangzhou-sales',
            parentId: 'east-division',
            parentName: '华东事业部',
            name: '杭州销售部',
            code: 'hangzhou_sales',
            nodeType: 'department',
            status: 'active',
            leader: { name: '许映川', username: 'xuyingchuan', initials: '许' },
            sortOrder: 200,
            directMembers: 17,
            roleCount: 1,
            businessReferenceCount: 9,
            remark: '负责浙江区域客户经营。',
            updatedAt: '2026-08-07 18:22',
            updatedBy: '许映川',
            children: [],
          },
        ],
      },
      {
        id: 'south-division',
        parentId: 'chengke-group',
        parentName: '澄客集团',
        name: '华南事业部',
        code: 'south_division',
        nodeType: 'department',
        status: 'active',
        leader: { name: '沈南乔', username: 'shennanqiao', initials: '沈' },
        sortOrder: 400,
        directMembers: 14,
        roleCount: 2,
        businessReferenceCount: 21,
        remark: '负责华南区域销售团队与客户经营。',
        updatedAt: '2026-08-09 10:03',
        updatedBy: '沈南乔',
        children: [
          {
            id: 'guangzhou-sales',
            parentId: 'south-division',
            parentName: '华南事业部',
            name: '广州销售部',
            code: 'guangzhou_sales',
            nodeType: 'department',
            status: 'active',
            leader: { name: '唐予安', username: 'tangyuan', initials: '唐' },
            sortOrder: 100,
            directMembers: 28,
            roleCount: 1,
            businessReferenceCount: 18,
            remark: '负责广州及周边客户经营。',
            updatedAt: '2026-08-08 14:15',
            updatedBy: '唐予安',
            children: [],
          },
          {
            id: 'shenzhen-channel',
            parentId: 'south-division',
            parentName: '华南事业部',
            name: '深圳渠道部',
            code: 'shenzhen_channel',
            nodeType: 'department',
            status: 'disabled',
            leader: null,
            sortOrder: 200,
            directMembers: 0,
            roleCount: 0,
            businessReferenceCount: 2,
            remark: '渠道业务已合并，当前节点保留历史记录。',
            updatedAt: '2026-08-06 17:40',
            updatedBy: '系统管理员',
            children: [],
          },
        ],
      },
    ],
  },
];

/**
 * 在组织树中查找指定部门。
 * @param nodes 当前需要检索的部门节点集合。
 * @param departmentId 目标部门 ID。
 * @returns 匹配的部门节点；不存在时返回 null。
 */
export function findDepartmentById(
  nodes: DepartmentNode[],
  departmentId: string,
): DepartmentNode | null {
  for (const node of nodes) {
    if (node.id === departmentId) return node;
    const childMatch = findDepartmentById(node.children, departmentId);
    if (childMatch) return childMatch;
  }

  return null;
}

/**
 * 计算当前部门的全部后代数量。
 * @param node 需要统计的部门节点。
 * @returns 所有层级后代节点总数。
 */
export function countDescendants(node: DepartmentNode): number {
  return node.children.reduce((total, child) => total + 1 + countDescendants(child), 0);
}

/**
 * 生成指定部门从集团根节点开始的完整可读路径。
 * @param nodes 当前组织树。
 * @param departmentId 目标部门 ID。
 * @param ancestors 当前递归层级已经经过的祖先名称。
 * @returns 使用斜线分隔的完整部门路径；目标不存在时返回空字符串。
 */
export function getDepartmentPath(
  nodes: DepartmentNode[],
  departmentId: string,
  ancestors: string[] = [],
): string {
  for (const node of nodes) {
    const currentPath = [...ancestors, node.name];
    if (node.id === departmentId) return currentPath.join(' / ');

    const childPath = getDepartmentPath(node.children, departmentId, currentPath);
    if (childPath) return childPath;
  }

  return '';
}

/**
 * 按关键词检索部门，并为每个结果保留可读祖先路径。
 * @param nodes 当前组织树。
 * @param keyword 部门名称或编码关键词。
 * @param ancestors 当前递归层级的祖先名称。
 * @returns 匹配节点及其祖先路径。
 */
export function searchDepartments(
  nodes: DepartmentNode[],
  keyword: string,
  ancestors: string[] = [],
): DepartmentSearchResult[] {
  const normalizedKeyword = keyword.trim().toLocaleLowerCase('zh-CN');
  if (!normalizedKeyword) return [];

  return nodes.flatMap((node) => {
    const nodeMatches = [node.name, node.code].some((value) =>
      value.toLocaleLowerCase('zh-CN').includes(normalizedKeyword),
    );
    const currentResult = nodeMatches
      ? [{ node, ancestorPath: ancestors.join(' / ') || '集团根节点' }]
      : [];

    return [
      ...currentResult,
      ...searchDepartments(node.children, keyword, [...ancestors, node.name]),
    ];
  });
}

/**
 * 向指定父节点追加一个演示子部门，并保持其他节点引用不变。
 * @param nodes 当前组织树。
 * @param parentId 新部门的父节点 ID。
 * @param child 需要追加的演示部门。
 * @returns 包含新部门的组织树副本。
 */
export function appendDepartmentChild(
  nodes: DepartmentNode[],
  parentId: string,
  child: DepartmentNode,
): DepartmentNode[] {
  return nodes.map((node) => {
    if (node.id === parentId) {
      return { ...node, children: [...node.children, child] };
    }

    return {
      ...node,
      children: appendDepartmentChild(node.children, parentId, child),
    };
  });
}
